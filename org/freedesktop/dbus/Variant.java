package org.freedesktop.dbus;

/**
 * A Wrapper class for Variant values. 
 * A method on DBus can send or receive a Variant. 
 * This will wrap another value whose type is determined at runtime.
 * The Variant may be parameterized to restrict the types it may accept.
 */
public class Variant<T>
{
   private final T o;
   /** Create a Variant. */
   public Variant(T o)
   {
      this.o = o;
   }
   /** Return the wrapped value. */
   public T getValue() { return o; }
   // this should work: public Class<? extends T> getType() { return o.getClass(); }
   /** Return the type of the wrapped value. */
   public Class getType() { return o.getClass(); }
   /** Format the Variant as a string. */
   public String toString() { return "["+o+"]"; }
}
