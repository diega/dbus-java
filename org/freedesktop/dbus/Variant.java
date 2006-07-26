package org.freedesktop.dbus;

import java.util.List;
import java.util.Map;

/**
 * A Wrapper class for Variant values. 
 * A method on DBus can send or receive a Variant. 
 * This will wrap another value whose type is determined at runtime.
 * The Variant may be parameterized to restrict the types it may accept.
 */
public class Variant<T> implements Comparable<Variant<T>>
{
   private final T o;
   /** 
    * Create a Variant. 
    * @throws IllegalArugmentException If you try and wrap Null or an inappropriate type.
    */
   public Variant(T o) throws IllegalArgumentException
   {
      if (null == o) throw new IllegalArgumentException("Can't wrap Null in a Variant");
      try {
         DBusConnection.getDBusType(o.getClass());
      } catch (DBusException DBe) {
         if (!(o instanceof List || o instanceof Map)) {
            if (DBusConnection.EXCEPTION_DEBUG) DBe.printStackTrace();
            throw new IllegalArgumentException("Can't wrap "+o.getClass()+" in a Variant ("+DBe.getMessage()+")");
         }
      }
      this.o = o;
   }
   /** Return the wrapped value. */
   public T getValue() { return o; }
   // this should work: public Class<? extends T> getType() { return o.getClass(); }
   /** Return the type of the wrapped value. */
   public Class getType() { return o.getClass(); }
   /** Format the Variant as a string. */
   public String toString() { return "["+o+"]"; }
   /** Compare this Variant with another by comparing contents */
   public boolean equals(Object other)
   {
      if (null == other) return false;
      if (!(other instanceof Variant)) return false;
      return this.o.equals(((Variant)other).o);
   }
   @SuppressWarnings("unchecked")
   public int compareTo(Variant<T> other)
   {
      return ((Comparable<T>) o).compareTo(other.getValue());
   }
}
