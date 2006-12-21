/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Vector;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;

class ListContainer
{
   private Object[] values;
   private String sig;
   private List<Object> list;
   public ListContainer(Object[] content, String sig) throws DBusException
   {
      Class c;
      try {
         Vector<Type> vt = new Vector<Type>();
         Marshalling.getJavaType(sig, vt, 1);
         if (vt.get(0) instanceof ParameterizedType)
            c = (Class) ((ParameterizedType) vt.get(0)).getRawType();
         else
            c = (Class) vt.get(0);

         if (Map.class.isAssignableFrom(c)) 
            c = MapContainer.class;
         if (List.class.isAssignableFrom(c)) 
            c = ListContainer.class;
         this.values = (Object[]) Array.newInstance(c, content.length);
      } catch (Exception CNFe) {
         if (DBusConnection.EXCEPTION_DEBUG) CNFe.printStackTrace();
         throw new DBusException("Map contains invalid type: "+CNFe.getMessage());
      }
      this.sig = sig;
      for (int i = 0; i < content.length; i++) {
         if (content[i].getClass().isArray()) {
            this.values[i] = new ListContainer(content[i]);
         } else
            this.values[i] = content[i];
      }
   }
   private ListContainer(Object content) throws DBusException
   {
      this.values = ArrayFrob.wrap(content);

      String[] s = Marshalling.getDBusType(content.getClass().getComponentType());
      if (1 != s.length) throw new DBusException("List Contents not single type");
      sig = s[0];
   }
   public ListContainer(Object[] content, Type t) throws DBusException
   {
      list = null;
      Class c;

      if (t instanceof Class)
         c = (Class) t;
      else if (t instanceof ParameterizedType)
         c = (Class) ((ParameterizedType) t).getRawType();
      else c = null;

      if (Map.class.isAssignableFrom(c))
         c = MapContainer.class;
      if (List.class.isAssignableFrom(c)) 
         c = ListContainer.class;

      values = (Object[]) Array.newInstance(c, content.length);
      try {
         for (int i = 0; i < content.length; i++) 
            values[i] = Marshalling.convertParameter(content[i], t);

      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException(e.getMessage());
      }

      String[] s = Marshalling.getDBusType(t);
      if (1 != s.length) throw new DBusException("List Contents not single type");
      sig = s[0];
   }
   public ListContainer(List<Object> l, ParameterizedType t) throws DBusException
   {
      Type[] ts = t.getActualTypeArguments();
      Class c;

      if (ts[0] instanceof Class)
         c = (Class) ts[0];
      else if (ts[0] instanceof ParameterizedType)
         c = (Class) ((ParameterizedType) ts[0]).getRawType();
      else c = null;

      if (Map.class.isAssignableFrom(c))
         c = MapContainer.class;
      if (List.class.isAssignableFrom(c)) 
         c = ListContainer.class;

      values = (Object[]) Array.newInstance(c, l.size());

      try {
         for (int i = 0; i < l.size(); i++) 
            values[i] = Marshalling.convertParameter(l.get(i), ts[0]);

      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException(e.getMessage());
      }

      String[] s = Marshalling.getDBusType(ts[0]);
      if (1 != s.length) throw new DBusException("List Contents not single type");
      sig = s[0];

      this.list = l;
   }
   public Object[] getValues() { return values; }
   public String getSig() { return sig; }
   public List getList(Type t) throws Exception
   { 
      if (null != list) return list;
      Type[] ts;
      if (t instanceof ParameterizedType)
         ts = ((ParameterizedType) t).getActualTypeArguments();
      else if (t instanceof GenericArrayType) {
         ts = new Type[1];
         ts[0] = ((GenericArrayType) t).getGenericComponentType();
      } else if (t instanceof Class && ((Class) t).isArray()) {
         ts = new Type[1];
         ts[0] = ((Class) t).getComponentType();
      } else {
         return null;
      }

      Object[] ov = new Object[values.length];
      for (int i = 0; i < values.length; i++) 
         ov[i] = Marshalling.deSerializeParameter(this.values[i], ts[0]);
      this.list = Arrays.asList(ov); 
      return list;
   }
}
