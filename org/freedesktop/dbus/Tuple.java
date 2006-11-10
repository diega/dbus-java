/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Vector;

/**
 * Class used for multiple return values.
 * When a D-Bus method returns multiple values this can't be directly
 * represented in Java. Instead you have to declare that the method 
 * returns a Tuple, and then annotate the method with the return type.
 * @see ReturnType
 */
public class Tuple
{
   private String sig;
   private Type[] types;
   private Object[] parameters;
   /**
    * Create a Tuple.
    * @param types The types of the following parameters, as an
    * array of Type objects. For complex types use the DBusMapType/DBusStructType/DBusListType
    * classes.
    * @param parameters The parameters being returned in this tuple.
    */
   public Tuple(Type[] types, Object... parameters) throws DBusExecutionException
   {
      try {
         this.types = types;
         StringBuffer sb = new StringBuffer();
         for (Type t: types) {
            String[] ss = DBusConnection.getDBusType(t);
            for (String s: ss)
               sb.append(s);
         }
         sig = sb.toString();
         this.parameters = DBusConnection.convertParameters(parameters, types);
      } catch (DBusException DBe) { throw new DBusExecutionException(DBe.getMessage());
      } catch (Exception e) { 
         throw new DBusExecutionException("Tuple construction failure: "+e.getMessage()); 
      }
   }
   /**
    * Create a Tuple.
    * @param typesig The D-Bus type string for this tuple.
    * @param parameters The parameters being returned in this tuple.
    */
   public Tuple(String typesig, Object... parameters) throws DBusExecutionException
   {
      try {
         this.sig = typesig;
         Vector<Type> v = new Vector<Type>();
         DBusConnection.getJavaType(typesig, v, -1);
         types = v.toArray(new Type[0]);
         this.parameters = DBusConnection.deSerializeParameters(parameters, types);
      } catch (DBusException DBe) { throw new DBusExecutionException(DBe.getMessage()); 
      } catch (Exception e) { 
         throw new DBusExecutionException("Tuple construction failure: "+e.getMessage()); 
      }
   }
   /**
    * Returns the D-Bus signature of the tuple.
    */
   public String getSig()
   {
      return sig;
   }
   /**
    * Returns the Java types of the tuple contents.
    */
   public Type[] getTypes()
   {
      return types;
   }
   /**
    * Returns all the contents of the tuple.
    */
   public Object[] getParameters()
   {
      return parameters;
   }
   /**
    * Returns the tuple element at the given index.
    * @param index Element to return.
    */
   public Object get(int index)
   {
      return parameters[index];
   }
   /**
    * Returns the tuple element type at the given index.
    * @param index Element to return type for.
    */
   public Type type(int index)
   {
      return types[index];
   }
   /**
    * Returns this Tuple represented as a String.
    */
   public String toString()
   {
      StringBuffer s = new StringBuffer();
      s.append("Tuple<");
      for (Type t: types) {
         if (t instanceof Class)
            s.append(((Class) t).getName());
         else if (t instanceof ParameterizedType)
            s.append(((Class) ((ParameterizedType) t).getRawType()).getName());
         s.append(',');
      }
      s.append("> {");
      for (Object o: parameters) {
         s.append(o.toString());
         s.append(',');
      }
      s.append('}');
      return s.toString();
   }
}
