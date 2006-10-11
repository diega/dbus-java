package org.freedesktop.dbus;

import java.lang.reflect.Type;
import java.util.Vector;

/**
 * A Wrapper class for Variant values. 
 * A method on DBus can send or receive a Variant. 
 * This will wrap another value whose type is determined at runtime.
 * The Variant may be parameterized to restrict the types it may accept.
 */
public class Variant<T>
{
   private final T o;
   private final Type type;
   private final String sig;
   /** 
    * Create a Variant from a basic type object.
    * @param o The wrapped value.
    * @throws IllegalArugmentException If you try and wrap Null or an object of a non-basic type.
    */
   public Variant(T o) throws IllegalArgumentException
   {
      if (null == o) throw new IllegalArgumentException("Can't wrap Null in a Variant");
      type = o.getClass();
      try {
         String[] ss = DBusConnection.getDBusType(o.getClass(), true);
         if (ss.length != 1)
         throw new IllegalArgumentException("Can't wrap a multi-valued type in a Variant ("+type+")");
         this.sig = ss[0];
      } catch (DBusException DBe) {
         if (DBusConnection.EXCEPTION_DEBUG) DBe.printStackTrace();
         throw new IllegalArgumentException("Can't wrap "+o.getClass()+" in an unqualified Variant ("+DBe.getMessage()+")");
      }
      this.o = o;
   }
   /** 
    * Create a Variant.
    * @param o The wrapped value.
    * @param type The explicit type of the value.
    * @throws IllegalArugmentException If you try and wrap Null or an object which cannot be sent over DBus.
    */
   public Variant(T o, Type type) throws IllegalArgumentException
   {
      if (null == o) throw new IllegalArgumentException("Can't wrap Null in a Variant");
      this.type = type;
      try {
         String[] ss = DBusConnection.getDBusType(type);
         if (ss.length != 1)
         throw new IllegalArgumentException("Can't wrap a multi-valued type in a Variant ("+type+")");
         this.sig = ss[0];
      } catch (DBusException DBe) {
         if (DBusConnection.EXCEPTION_DEBUG) DBe.printStackTrace();
         throw new IllegalArgumentException("Can't wrap a "+type+" in a Variant ("+DBe.getMessage()+")");
      }
      this.o = o;
   }
   /** 
    * Create a Variant.
    * @param o The wrapped value.
    * @param type The explicit type of the value, as a dbus type string.
    * @throws IllegalArugmentException If you try and wrap Null or an object which cannot be sent over DBus.
    */
   public Variant(T o, String sig) throws IllegalArgumentException
   {
      if (null == o) throw new IllegalArgumentException("Can't wrap Null in a Variant");
      this.sig = sig;
      try {
         Vector<Type> ts = new Vector<Type>();
         DBusConnection.getJavaType(sig, ts,1);
         if (ts.size() != 1)
            throw new IllegalArgumentException("Can't wrap multiple or no types in a Variant ("+sig+")");
         this.type = ts.get(0);
      } catch (DBusException DBe) {
         if (DBusConnection.EXCEPTION_DEBUG) DBe.printStackTrace();
         throw new IllegalArgumentException("Can't wrap a "+sig+" in a Variant ("+DBe.getMessage()+")");
      }
      this.o = o;
      System.err.println("DEBUG: Created variant Type: "+type+", sig: "+sig);
   }
   /** Return the wrapped value. */
   public T getValue() { return o; }
   /** Return the type of the wrapped value. */
   public Type getType() { return type; }
   /** Return the dbus signature of the wrapped value. */
   public String getSig() { return sig; }
   /** Format the Variant as a string. */
   public String toString() { return "["+o+"]"; }
   /** Compare this Variant with another by comparing contents */
   public boolean equals(Object other)
   {
      if (null == other) return false;
      if (!(other instanceof Variant)) return false;
      return this.o.equals(((Variant)other).o);
   }
}
