package org.freedesktop.dbus;

class MethodTuple
{
   String name;
   String sig;
   public MethodTuple(String name, String sig)
   {
      this.name = name;
      this.sig = sig;
   }
   public boolean equals(Object o)
   {
      return o.getClass().equals(MethodTuple.class)
            && ((MethodTuple) o).name.equals(this.name)
            && ((MethodTuple) o).sig.equals(this.sig);
   }
   public int hashCode()
   {
      return name.hashCode()+sig.hashCode();
   }
}
