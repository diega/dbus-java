package org.freedesktop.dbus;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import org.freedesktop.dbus.Position;

/**
 * This class should be extended to create Structs.
 * Any such class may be sent over DBus to a method which takes a Struct.
 * All fields in the Struct which you wish to be serialized and sent to the
 * remote method should be annotated with the org.freedesktop.dbus.Position
 * annotation, in the order they should appear in to Struct to DBus.
 */
public abstract class Struct
{
   private static Map<Type,Type[]> typecache = new HashMap<Type,Type[]>();
   static void putStructTypeCache(Type k, Type[] v)
   {
      typecache.put(k, v);
   }
   static Type[] getStructTypeCache(Type k)
   {
      return typecache.get(k);
   }
   private String sig = null;
   private Object[] parameters = null;
   public Struct() {}
   private void setup() throws DBusException
   {
      Field[] fs = getClass().getDeclaredFields();
      Object[] args = new Object[fs.length];
      Type[] ts = new Type[fs.length];
      int diff = 0;
      for (Field f : fs) {
         Position p = f.getAnnotation(Position.class);
         if (null == p) {
            diff++;
            continue;
         }
         ts[p.value()] = f.getGenericType();
         try {
            args[p.value()] = DBusConnection.convertParameters(
                  new Object[] { f.get(this) },
                  new Type[] { ts[p.value()] })[0];
         } catch (Exception e) {
            if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
            throw new DBusException(e.getMessage());
         }
      }
         
      sig = "";
      for (Type t: ts)
         if (null != t)
            for (String s: DBusConnection.getDBusType(t))
               sig += s;

      this.parameters = new Object[args.length - diff];
      for (int i = 0; i < parameters.length; i++)
         parameters[i] = args[i];
   }
   /**
    * Returns the struct contents in order.
    * @throws DBusException If there is  a problem doing this.
    */
   public final Object[] getParameters() throws DBusException
   {
      if (null != parameters) return parameters;
      setup();
      return parameters;
   }
   /**
    * Returns the DBus signatures of the struct contents.
    * @throws DBusException If there is  a problem doing this.
    */
   public final String getSig() throws DBusException
   {
      if (null != sig) return sig;
      setup();
      return sig;
   }
   /** Returns this struct as a string. */
   public final String toString()
   {
      String s = getClass().getName()+"<";
      if (null == parameters)
         return s+"not setup>";
      if (0 == parameters.length) 
         return s+">";
      for (Object o: parameters)
         s += o+", ";
      return s.replaceAll(", $", ">");
   }
}
