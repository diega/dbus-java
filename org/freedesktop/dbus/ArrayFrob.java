package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

class ArrayFrob
{
   static Hashtable<Class, Class> primitiveToWrapper = new Hashtable<Class, Class>();
   static Hashtable<Class, Class> wrapperToPrimitive = new Hashtable<Class, Class>();
   static {
      primitiveToWrapper.put( Boolean.TYPE, Boolean.class );
      primitiveToWrapper.put( Byte.TYPE, Byte.class );
      primitiveToWrapper.put( Short.TYPE, Short.class );
      primitiveToWrapper.put( Character.TYPE, Character.class );
      primitiveToWrapper.put( Integer.TYPE, Integer.class );
      primitiveToWrapper.put( Long.TYPE, Long.class );
      primitiveToWrapper.put( Float.TYPE, Float.class );
      primitiveToWrapper.put( Double.TYPE, Double.class );
      wrapperToPrimitive.put( Boolean.class, Boolean.TYPE );
      wrapperToPrimitive.put( Byte.class, Byte.TYPE );
      wrapperToPrimitive.put( Short.class, Short.TYPE );
      wrapperToPrimitive.put( Character.class, Character.TYPE );
      wrapperToPrimitive.put( Integer.class, Integer.TYPE );
      wrapperToPrimitive.put( Long.class, Long.TYPE );
      wrapperToPrimitive.put( Float.class, Float.TYPE );
      wrapperToPrimitive.put( Double.class, Double.TYPE );

   }
   public static Object[] wrap(Object o) throws IllegalArgumentException
   {
         Class ac = o.getClass();
         if (!ac.isArray()) throw new IllegalArgumentException("Not an array");
         Class cc = ac.getComponentType();
         Class ncc = primitiveToWrapper.get(cc);
         if (null == ncc) throw new IllegalArgumentException("Not a primitive type");
         Object[] ns = (Object[]) Array.newInstance(ncc, Array.getLength(o));
         for (int i = 0; i < ns.length; i++)
            ns[i] = Array.get(o, i);
         return ns;
   }
   public static Object unwrap(Object[] ns) throws IllegalArgumentException
   {
      Class<? extends Object[]> ac = ns.getClass();
      Class cc = ac.getComponentType();
      Class ncc = wrapperToPrimitive.get(cc);
      if (null == ncc) throw new IllegalArgumentException("Not a wrapper type");
      Object o = Array.newInstance(ncc, ns.length);
      for (int i = 0; i < ns.length; i++)
         Array.set(o, i, ns[i]);
      return o;
   }
   public static List listify(Object[] ns) throws IllegalArgumentException
   {
      return Arrays.asList(ns);
   }
   public static List listify(Object o) throws IllegalArgumentException
   {
      if (o instanceof Object[]) return listify((Object[]) o);
      if (!o.getClass().isArray()) throw new IllegalArgumentException("Not an array");
      List<Object> l = new ArrayList<Object>(Array.getLength(o));
      for (int i = 0; i < Array.getLength(o); i++)
         l.add(Array.get(o, i));
      return l;
   }
   @SuppressWarnings("unchecked")
   public static <T> T[] delist(List l, Class<T> c) throws IllegalArgumentException
   {
      return (T[]) l.toArray((T[]) Array.newInstance(c, 0));
   }
   public static Object delistprimitive(List l, Class c) throws IllegalArgumentException
   {
      Object o = Array.newInstance(c, l.size());
      for (int i = 0; i < l.size(); i++)
         Array.set(o, i, l.get(i));
      return o;
   }
   public static Object convert(Object o, Class<? extends Object> c) throws IllegalArgumentException
   {
      /* Possible Conversions:
       *
       ** List<Integer> -> List<Integer>
       ** List<Integer> -> int[]
       ** List<Integer> -> Integer[]
       ** int[] -> int[]
       ** int[] -> List<Integer>
       ** int[] -> Integer[]
       ** Integer[] -> Integer[]
       ** Integer[] -> int[]
       ** Integer[] -> List<Integer>
       */
      try {
         // List<Integer> -> List<Integer>
         if (List.class.equals(c) 
               && o instanceof List)
            return o;

         // int[] -> List<Integer>
         // Integer[] -> List<Integer>
         if (List.class.equals(c) 
               && o.getClass().isArray()) 
            return listify(o);

         // int[] -> int[]
         // Integer[] -> Integer[]
         if (o.getClass().isArray() 
               && c.isArray() 
               && o.getClass().getComponentType().equals(c.getComponentType()))
            return o;

         // int[] -> Integer[]
         if (o.getClass().isArray() 
               && c.isArray() 
               && o.getClass().getComponentType().isPrimitive()) 
            return wrap(o);

         // Integer[] -> int[]
         if (o.getClass().isArray()
               && c.isArray() 
               && c.getComponentType().isPrimitive()) 
            return unwrap((Object[]) o);

         // List<Integer> -> int[]
         if (o instanceof List 
               && c.isArray() 
               && c.getComponentType().isPrimitive()) 
            return delistprimitive((List) o, c.getComponentType());

         // List<Integer> -> Integer[]
         if (o instanceof List 
                && c.isArray()) 
             return delist((List) o, c.getComponentType());

         if (o.getClass().isArray()
               && c.isArray())
            return type((Object[]) o, c.getComponentType());
      
      } catch (Exception e) {
         throw new IllegalArgumentException(e);
      }

      throw new IllegalArgumentException("Not An Expected Convertion type from "+o.getClass()+" to "+c);
   }
   public static Object[] type(Object[] old, Class c)
   {
      Object[] ns = (Object[]) Array.newInstance(c, old.length);
      for (int i = 0; i < ns.length; i++)
         ns[i] = old[i];
      return ns;
   }
}
