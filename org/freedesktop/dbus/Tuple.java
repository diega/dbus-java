package org.freedesktop.dbus;

public abstract class Tuple
{
   private final Object[] parameters;
   public Tuple(Object... parameters)
   {
      this.parameters = parameters;
   }
   public final Object[] getParameters()
   {
      return parameters;
   }
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
