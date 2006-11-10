/*
   D-Bus Java Bindings
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

class RemoteInvocationHandler implements InvocationHandler
{
   public static Object convertRV(String sig, Object[] rp, Method m) throws DBusException
   {
      Class c = m.getReturnType();

      if (null == rp) { 
         if(null == c || Void.TYPE.equals(c)) return null;
         else throw new DBusExecutionException("Wrong return type (expected void)");
      }

      switch (rp.length) {
         case 0:
            if (null == c || Void.TYPE.equals(c))
               return null;
            else throw new DBusExecutionException("Wrong return type (expected void)");
         case 1:

            try { 
               rp = DBusConnection.deSerializeParameters(rp, 
                     new Type[] { m.getGenericReturnType() });
            }
            catch (Exception e) { 
               if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
               throw new DBusExecutionException("Wrong return type (failed to de-serialize correct types: "+e.getMessage()+")");
            }

            return rp[0];
         default:

            // check we are meant to return multiple values
            if (!Tuple.class.isAssignableFrom(c))
               throw new DBusExecutionException("Wrong return type (not expecting Tuple)");
            try {
               rp = DBusConnection.deSerializeParameters(rp, ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments());
            } catch (Exception e) { 
               if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
               throw new DBusExecutionException("Wrong return type (failed to de-serialize correct types: "+e.getMessage()+")");
            }
            Constructor cons = c.getConstructors()[0];
            try {
               return cons.newInstance(rp);
            } catch (Exception e) {
               if (DBusConnection.EXCEPTION_DEBUG)
                  e.printStackTrace();
               throw new DBusException(e.getMessage());
            }
      }
   }
   public static Object executeRemoteMethod(RemoteObject ro, Method m, DBusConnection conn, boolean async, Object... args) throws DBusExecutionException
   {
      Type[] ts = m.getGenericParameterTypes();
      try {
         args = DBusConnection.convertParameters(args, ts);
      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusExecutionException(e.getMessage());
      }
      MethodCall call;
      if (null == ro.iface)
         call = new MethodCall(ro.busname, ro.objectpath, null, m.getName(), args);
      else
         call = new MethodCall(ro.busname, ro.objectpath, DBusConnection.dollar_pattern.matcher(ro.iface.getName()).replaceAll("."), m.getName(), args);
      if (ro.autostart) call.setFlags(MethodCall.AUTO_START);
      if (async) call.setFlags(MethodCall.ASYNC);
      if (m.isAnnotationPresent(DBus.Method.NoReply.class)) call.setFlags(MethodCall.NO_REPLY);
      synchronized (conn.outgoing) {
         conn.outgoing.add(call);
      }

      if (async) return new DBusAsyncReply(call, m);

      // get reply
      if (m.isAnnotationPresent(DBus.Method.NoReply.class)) return null;

      DBusMessage reply = call.getReply();
      if (null == reply) throw new NoReply("No reply within specified time");
               
      if (reply instanceof DBusErrorMessage)
         ((DBusErrorMessage) reply).throwException();

      try {
         return convertRV(reply.getSig(), reply.getParameters(), m);
      } catch (DBusException e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
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

