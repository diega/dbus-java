package org.freedesktop.dbus;

public class Variant<T>
{
   private T o;
   public Variant(T o)
   {
      this.o = o;
   }
   public T getValue() { return o; }
   // this should work: public Class<? extends T> getType() { return o.getClass(); }
   public Class getType() { return o.getClass(); }
   public String toString() { return "["+o+"]"; }
}
