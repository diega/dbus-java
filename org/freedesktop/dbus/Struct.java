package org.freedesktop.dbus;

public abstract class Struct
{
   private String sig;
   private Object[] parameters;
   public Struct(Object... parameters) throws DBusException
   {
      this.parameters = parameters;
      sig = "";
      for (Object o: parameters)
         sig += DBusConnection.getDBusType(o.getClass());
   }
   public final Object[] getParameters() { return parameters; }
   public final String getSig() { return sig; }
   public final String toString()
   {
      String s = getClass().getName()+"<";
      Object[] os = getParameters();
      if (null == os || 0 == os.length) {
         return s+">";
      }
      for (Object o: os)
         s += o+", ";
      return s.replaceAll(", $", ">");
   }
}
