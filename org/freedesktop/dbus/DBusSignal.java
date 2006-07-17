package org.freedesktop.dbus;

import java.util.List;
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
   /** The path to the object this is emitted from */
   protected String objectpath;
   DBusSignal(String source, String objectpath, String type, String name, String sig, Object[] parameters, long serial)
   {
      super(source, type, name, sig, parameters, serial);
      this.objectpath = objectpath;
   }
   static DBusSignal createSignal(Class<? extends DBusSignal> c, String source, String objectpath, String sig, long serial, Object... parameters) throws DBusException
   {
      Constructor con = c.getDeclaredConstructors()[0];
      Type[] ts = con.getGenericParameterTypes();
      Type[] types = new Type[ts.length-1];
      for (int i = 1; i < ts.length; i++)
         if (ts[i] instanceof TypeVariable)
            for (Type b: ((TypeVariable) ts[i]).getBounds())
               types[i-1] = b;
         else
            types[i-1] = ts[i];

      try {
         parameters = DBusConnection.deSerializeParameters(parameters, types);
         if (null == parameters) return (DBusSignal) con.newInstance(objectpath);
         else {
            Object[] args = new Object[parameters.length + 1];
            args[1] = objectpath;
            for (int i = 1; i < args.length; i++)
               args[i] = parameters[i-1];

            return (DBusSignal) con.newInstance(args);
         }
      } catch (Exception e) { 
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
      try {
         name = getClass().getSimpleName();
         if (null != getClass().getEnclosingClass())
            type = DBusConnection.dollar_pattern.matcher(getClass().getEnclosingClass().getName()).replaceAll(".");
         this.objectpath = objectpath;

         // convert recursively everything
         Type[] ts = getClass().getDeclaredConstructors()[0].getGenericParameterTypes();
         Type[] types = new Type[ts.length-1];
         for (int i = 1; i <= types.length; i++) 
            if (ts[i] instanceof TypeVariable)
               types[i-1] = ((TypeVariable) ts[i]).getBounds()[0];
            else
               types[i-1] = ts[i];
         if (null != parameters)
            parameters = DBusConnection.convertParameters(parameters, types);

      } catch (Exception e) {
         throw new DBusException("Failed to correctly determine DBusSignal type: "+e);
      }
   }
   /** Returns the path to the object this is emitted from. */
   public String getObjectPath() { return objectpath; }

}
