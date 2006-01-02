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

class RemoteInvocationHandler implements InvocationHandler
{

   public static Object executeRemoteMethod(RemoteObject ro, Method m, DBusConnection conn, Class<? extends DBusInterface> iface, Object... args) throws DBusExecutionException
   {
      Type[] ts = m.getGenericParameterTypes();
      try {
         args = DBusConnection.convertParameters(args, ts);
      } catch (Exception e) {
         e.printStackTrace();
         throw new DBusExecutionException(e.getMessage());
      }
      MethodCall call = new MethodCall(ro.service, ro.objectpath, ro.iface, m.getName(), args);
      synchronized (conn.outgoing) {
         conn.outgoing.add(call);
      }

      Class c = m.getReturnType();

      // get reply
      if (c.equals(DBusNoReply.class)) return null;

      DBusMessage reply = call.getReply();
               
      Object[] rp = reply.getParameters();

      if (reply instanceof DBusErrorMessage) {
         if (null == rp || 0 == rp.length || null == rp[0])
            throw new DBusExecutionException("Got Error Message in Reply");
         else
            throw new DBusExecutionException(""+rp[0]);
      }

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
               rp = DBusConnection.deSerialiseParameters(rp, 
                     new Type[] { m.getGenericReturnType() });
            }
            catch (Exception e) { 
               throw new DBusExecutionException("Wrong return type (failed to de-serialise correct types: "+e.getMessage()+")");
            }

            return rp[0];
         default:

            // check we are meant to return multiple values
            if (!Tuple.class.isAssignableFrom(c))
               throw new DBusExecutionException("Wrong return type (not expecting Tuple)");
            
            ParameterizedType p = (ParameterizedType) m.getGenericReturnType();
            
            // check we have the correct number of args
            ts = p.getActualTypeArguments();
            if (ts.length != rp.length) 
               throw new DBusExecutionException("Incorrect number of return values. Expected "+ts.length+" got "+rp.length);

            Constructor con;
            try { 
               // convert everything to the correct types
               rp = DBusConnection.deSerialiseParameters(rp, ts);

               con = ((Class) p.getRawType()).getDeclaredConstructors()[0]; 
            }
            catch (Exception e) { 
               throw new DBusExecutionException("Wrong return type (Tuple type invalid: "+e.getMessage()+")");
            }

            // create tuple and return stuff
            try { return con.newInstance(rp); }
            catch (Exception e) {
               throw new DBusExecutionException("Wrong return type (failed to create Tuple, contents probably invalid)");
            }
      }
   }

   DBusConnection conn;
   RemoteObject remote;
   Class<? extends DBusInterface> iface;
   public RemoteInvocationHandler(DBusConnection conn, RemoteObject remote, Class<? extends DBusInterface> iface)
   {
      this.iface = iface;
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
      else if (method.getName().equals("getClass")) return iface;
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
         return remote.service+":"+remote.objectpath+":"+remote.iface;

      return executeRemoteMethod(remote, method, conn, iface, args);
   }
}

