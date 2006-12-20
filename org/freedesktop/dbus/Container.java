/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.freedesktop.dbus.exceptions.DBusException;

/**
 * This class is the super class of both Structs and Tuples 
 * and holds common methods.
 */
abstract class Container
{
   private static Map<Type,Type[]> typecache = new HashMap<Type,Type[]>();
   static void putTypeCache(Type k, Type[] v)
   {
      typecache.put(k, v);
   }
   static Type[] getTypeCache(Type k)
   {
      return typecache.get(k);
   }
   private String sig = null;
   private Object[] parameters = null;
   public Container() {}
   private void setup(Type[] types) throws DBusException
   {
      Field[] fs = getClass().getDeclaredFields();
      Object[] args = new Object[fs.length];
      Type[] ts = types;
      if (null == ts) ts = getTypeCache(getClass());
      if (null == ts) ts = new Type[fs.length];

      int diff = 0;
      for (Field f : fs) {
         Position p = f.getAnnotation(Position.class);
         if (null == p) {
            diff++;
            continue;
         }
         if (null == ts[p.value()])
            ts[p.value()] = f.getGenericType();
         try {
            args[p.value()] = Marshalling.convertParameters(
                  new Object[] { f.get(this) },
                  new Type[] { ts[p.value()] })[0];
         } catch (Exception e) {
            if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
            throw new DBusException(e.getMessage());
         }
      }
         
      sig = "";
      for (Type t: ts)
         if (null != t)
            for (String s: Marshalling.getDBusType(t))
               sig += s;

      this.parameters = new Object[args.length - diff];
      for (int i = 0; i < parameters.length; i++)
         parameters[i] = args[i];
   }
   /**
    * Returns the struct contents in order.
    * @throws DBusException If there is  a problem doing this.
    */
   public final Object[] getParameters() throws DBusException
   {
      if (null != parameters) return parameters;
      setup(null);
      return parameters;
   }
   final Object[] getParameters(Type[] ts) throws DBusException
   {
      if (null != parameters) return parameters;
      setup(ts);
      return parameters;
   }
   /**
    * Returns the DBus signatures of the struct contents.
    * @throws DBusException If there is  a problem doing this.
    */
   public final String getSig() throws DBusException
   {
      if (null != sig) return sig;
      setup(null);
      return sig;
   }
   /** Returns this struct as a string. */
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
   public final boolean equals(Object other)
   {
      try {
         if (other instanceof Container)  {
            Container that = (Container) other;
            if (this.getClass().equals(that.getClass()))
               return Arrays.equals(this.getParameters(), that.getParameters());
            else return false;
         }
         else return false;
      } catch (DBusException DBe) {
         if (DBusConnection.EXCEPTION_DEBUG)
            DBe.printStackTrace();
         return false;
      }
   }
}
