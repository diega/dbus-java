package org.freedesktop.dbus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/** 
 * A Signal on DBus.
 * Signals should all extend this class. They MUST provide a 0-arg constructor, and any other
 * constructor they provide MUST call the super constructor with signature (String, String, Object...)
 */
public abstract class DBusSignal extends DBusMessage
{
   private static Map<Class, Type[]> typeCache = new HashMap<Class, Type[]>();
   private static Map<Class, Constructor> conCache = new HashMap<Class, Constructor>();
   /** The path to the object this is emitted from */
   protected String objectpath;
   DBusSignal(String source, String objectpath, String type, String name, String sig, Object[] parameters, long serial)
   {
      super(source, type, name, sig, parameters, serial);
      this.objectpath = objectpath;
   }
   static DBusSignal createSignal(Class<? extends DBusSignal> c, String source, String objectpath, String sig, long serial, Object... parameters) throws DBusException
   {
      Type[] types = typeCache.get(c);
      Constructor con = conCache.get(c);
      if (null == types) {
         con = c.getDeclaredConstructors()[0];
         conCache.put(c, con);
         Type[] ts = con.getGenericParameterTypes();
         types = new Type[ts.length-1];
         for (int i = 1; i < ts.length; i++)
            if (ts[i] instanceof TypeVariable)
               for (Type b: ((TypeVariable) ts[i]).getBounds())
                  types[i-1] = b;
            else
               types[i-1] = ts[i];
         typeCache.put(c, types);
      }

      try {
         parameters = DBusConnection.deSerializeParameters(parameters, types);
         DBusSignal s;
         if (null == parameters) s = (DBusSignal) con.newInstance(objectpath);
         else {
            Object[] args = new Object[parameters.length + 1];
            args[0] = objectpath;
            System.arraycopy(parameters, 0, args, 1, parameters.length);

            s = (DBusSignal) con.newInstance(args);
         }
         s.setSerial(serial);
         s.setSource(source);
         return s;
      } catch (Exception e) { 
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException(e.getMessage());
      }
   }
   /** 
    * Create a new signal.
    * This contructor MUST be called by all sub classes.
    * @param objectpath The path to the object this is emitted from.
    * @param parameters The parameters of the signal.
    * @throws DBusException This is thrown if the subclass is incorrectly defined.
    */
   protected DBusSignal(String objectpath, Object... parameters) throws DBusException
   {
      super(null, "", null, "", parameters, 0);
      if (!objectpath.matches(DBusConnection.OBJECT_REGEX)) throw new DBusException("Invalid object path");
      Class tc = getClass();
      try {
         name = tc.getSimpleName();
         if (null != tc.getEnclosingClass())
            type = DBusConnection.dollar_pattern.matcher(tc.getEnclosingClass().getName()).replaceAll(".");
         this.objectpath = objectpath;

         // convert recursively everything
         Type[] types = typeCache.get(tc);
         if (null == types) {
            Constructor con = tc.getDeclaredConstructors()[0];
            conCache.put(tc, con);
            Type[] ts = con.getGenericParameterTypes();
            types = new Type[ts.length-1];
            for (int i = 1; i <= types.length; i++) 
               if (ts[i] instanceof TypeVariable)
                  types[i-1] = ((TypeVariable) ts[i]).getBounds()[0];
               else
                  types[i-1] = ts[i];

            typeCache.put(tc, types);
         }
         if (null != parameters)
            parameters = DBusConnection.convertParameters(parameters, types);

      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException("Failed to correctly determine DBusSignal type: "+e);
      }
   }
   /** Returns the path to the object this is emitted from. */
   public String getObjectPath() { return objectpath; }

}
