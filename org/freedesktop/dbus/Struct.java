package org.freedesktop.dbus;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import org.freedesktop.dbus.Position;

public abstract class Struct
{
   private String sig = null;
   private Object[] parameters = null;
   public Struct() {}
   private void setup() throws DBusException
   {
      Field[] fs = getClass().getDeclaredFields();
      this.parameters = new Object[fs.length];
      Type[] ts = new Type[fs.length];
      for (Field f : fs) {
         Position p = f.getAnnotation(Position.class);
         if (null == p) throw new DBusException("Struct not annotated with field order");
         ts[p.value()] = f.getGenericType();
         try {
            parameters[p.value()] = DBusConnection.convertParameters(
                  new Object[] { f.get(this) },
                  new Type[] { ts[p.value()] })[0];
         } catch (Exception e) {
            throw new DBusException(e.getMessage());
         }
      }
         
      sig = "";
      for (Type t: ts)
         sig += DBusConnection.getDBusType(t);
   }
   public final Object[] getParameters() throws DBusException
   {
      if (null != parameters) return parameters;
      setup();
      return parameters;
   }
   public final String getSig() throws DBusException
   {
      if (null != sig) return sig;
      setup();
      return sig;
   }
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
