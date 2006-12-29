/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

import org.freedesktop.DBus;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.NotConnected;

import cx.ath.matthew.debug.Debug;

class RemoteInvocationHandler implements InvocationHandler
{
   public static Object convertRV(String sig, Object[] rp, Method m, DBusConnection conn) throws DBusException
   {
      Class c = m.getReturnType();

      if (null == rp) { 
         if(null == c || Void.TYPE.equals(c)) return null;
         else throw new DBusExecutionException("Wrong return type (got void, expected a value)");
      }

      switch (rp.length) {
         case 0:
            if (null == c || Void.TYPE.equals(c))
               return null;
            else throw new DBusExecutionException("Wrong return type (got void, expected a value)");
         case 1:
            try { 
               rp = Marshalling.deSerializeParameters(rp, 
                     new Type[] { m.getGenericReturnType() }, conn);
            }
            catch (Exception e) { 
               if (DBusConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
               throw new DBusExecutionException("Wrong return type (failed to de-serialize correct types: "+e.getMessage()+")");
            }

            return rp[0];
         default:

            // check we are meant to return multiple values
            if (!Tuple.class.isAssignableFrom(c))
               throw new DBusExecutionException("Wrong return type (not expecting Tuple)");
            try {
               rp = Marshalling.deSerializeParameters(rp, ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments(), conn);
            } catch (Exception e) { 
               if (DBusConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
               throw new DBusExecutionException("Wrong return type (failed to de-serialize correct types: "+e.getMessage()+")");
            }
            Constructor cons = c.getConstructors()[0];
            try {
               return cons.newInstance(rp);
            } catch (Exception e) {
               if (DBusConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
               throw new DBusException(e.getMessage());
            }
      }
   }
   public static Object executeRemoteMethod(RemoteObject ro, Method m, DBusConnection conn, boolean async, Object... args) throws DBusExecutionException
   {
      Type[] ts = m.getGenericParameterTypes();
      String sig = null;
      if (ts.length > 0) try {
         sig = Marshalling.getDBusType(ts);
         args = Marshalling.convertParameters(args, ts, conn);
      } catch (DBusException DBe) {
         throw new DBusExecutionException("Failed to construct D-Bus type: "+DBe.getMessage());
      }
      MethodCall call;
      byte flags = 0;
      if (!ro.autostart) flags |= Message.Flags.NO_AUTO_START;
      if (async) flags |= Message.Flags.ASYNC;
      if (m.isAnnotationPresent(DBus.Method.NoReply.class)) flags |= Message.Flags.NO_REPLY_EXPECTED;
      try {
         if (null == ro.iface)
            call = new MethodCall(ro.busname, ro.objectpath, null, m.getName(),flags, sig, args);
         else
            call = new MethodCall(ro.busname, ro.objectpath, DBusConnection.dollar_pattern.matcher(ro.iface.getName()).replaceAll("."), m.getName(), flags, sig, args);
      } catch (DBusException DBe) {
         if (DBusConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
         throw new DBusExecutionException("Failed to construct outgoing method call: "+DBe.getMessage());
      }
      if (null == conn.outgoing) throw new NotConnected("Not Connected");
      synchronized (conn.outgoing) {
         conn.outgoing.add(call);
      }

      if (async) return new DBusAsyncReply(call, m, conn);

      // get reply
      if (m.isAnnotationPresent(DBus.Method.NoReply.class)) return null;

      Message reply = call.getReply();
      if (null == reply) throw new DBus.Error.NoReply("No reply within specified time");
               
      if (reply instanceof Error)
         ((Error) reply).throwException();

      try {
         return convertRV(reply.getSig(), reply.getParameters(), m, conn);
      } catch (DBusException e) {
         if (DBusConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
         throw new DBusExecutionException(e.getMessage());
      }
   }

   DBusConnection conn;
   RemoteObject remote;
   public RemoteInvocationHandler(DBusConnection conn, RemoteObject remote)
   {
      this.remote = remote;
      this.conn = conn;
   }
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if (method.getName().equals("isRemote")) return true;
      else if (method.getName().equals("clone")) return null;
      else if (method.getName().equals("equals")) {
         try { 
            if (1 == args.length)   
               return new Boolean(remote.equals(((RemoteInvocationHandler) Proxy.getInvocationHandler(args[0])).remote));
         } catch (IllegalArgumentException IAe) {
            return Boolean.FALSE;
         }
      }
      else if (method.getName().equals("finalize")) return null;
      else if (method.getName().equals("getClass")) return DBusInterface.class;
      else if (method.getName().equals("hashCode")) return remote.hashCode();
      else if (method.getName().equals("notify")) {
         remote.notify();
         return null;
      } else if (method.getName().equals("notifyAll")) {
         remote.notifyAll();
         return null;
      } else if (method.getName().equals("wait"))  {
         if (0 == args.length) remote.wait();
         else if (1 == args.length 
               && args[0] instanceof Long) remote.wait((Long) args[0]);
         else if (2 == args.length 
               && args[0] instanceof Long
               && args[1] instanceof Integer) 
            remote.wait((Long) args[0], (Integer) args[1]);
         if (args.length <= 2)
            return null;
      }
      else if (method.getName().equals("toString"))
         return remote.toString();

      return executeRemoteMethod(remote, method, conn, false, args);
   }
}

