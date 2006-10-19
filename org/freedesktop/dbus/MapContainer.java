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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class MapContainer
{
   /*
    * key[i] => value[i]
    */
   private Object[] keys;
   private Object[] values;
   private String sig;
   private Map<Object,Object> map;
   public MapContainer(Object[][] content, String sig) throws DBusException
   {
      Class[] cs = new Class[2];
      try {
         sig = sig.substring(1);
         String name = DBusConnection.getJavaType(sig, null, null, true, true);
         cs[0] = Class.forName(name);
         this.keys = (Object[]) Array.newInstance(cs[0], content.length);
         sig = sig.substring(1);
         name = DBusConnection.getJavaType(sig, null, null, true, true);
         cs[1] = Class.forName(name);
         if (Map.class.isAssignableFrom(cs[1])) 
            cs[1] = MapContainer.class;
         if (List.class.isAssignableFrom(cs[1])) 
            cs[1] = ListContainer.class;
         this.values = (Object[]) Array.newInstance(cs[1], content.length);
      } catch (ClassNotFoundException CNFe) {
         if (DBusConnection.EXCEPTION_DEBUG) CNFe.printStackTrace();
         throw new DBusException("Map contains invalid type: "+CNFe.getMessage());
      }
      this.sig = sig;
      for (int i = 0; i < content.length; i++) {
         this.keys[i] = content[i][0];
         this.values[i] = content[i][1];
      }
   }
   public MapContainer(Object[] keys, Object[] values, String sig) throws DBusException
   {
      this.keys = keys;
      this.values = values;
      this.sig = sig;
      this.map = new DBusMap<Object,Object>(keys,values);
   }
   public MapContainer(Map<Object,Object> m, ParameterizedType t) throws DBusException
   {
      Type[] ts = t.getActualTypeArguments();
      Class c;
      sig = "{";

      c = (Class) ts[0];
      //keys = m.keySet().toArray((Object[]) Array.newInstance(c, 0));
      keys = (Object[]) Array.newInstance(c, m.size());

      String[] s = DBusConnection.getDBusType(ts[0]);
      if (1 != s.length) throw new DBusException("List Contents not single type");
      sig += s[0];

      if (ts[1] instanceof Class)
         c = (Class) ts[1];
      else if (ts[1] instanceof ParameterizedType)
         c = (Class) ((ParameterizedType) ts[1]).getRawType();

      if (Map.class.isAssignableFrom(c))
         c = MapContainer.class;
      if (List.class.isAssignableFrom(c)) 
         c = ListContainer.class;

      values = (Object[]) Array.newInstance(c, m.size());

      try {
         int i = 0;
         for (Map.Entry e: m.entrySet()) {
            values[i] = DBusConnection.convertParameter( e.getValue(), ts[1]);
            keys[i] = e.getKey();
            i++;
         }
      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException(e.getMessage());
      }

      s = DBusConnection.getDBusType(ts[1]);
      if (1 != s.length) throw new DBusException("List Contents not single type");
      sig += s[0];
      sig += '}';

      this.map = m;
   }
   public Object[] getKeys() { return keys; }
   public Object[] getValues() { return values; }
   public String getSig() { return sig; }
   public Map getMap(Type t) throws Exception
   { 
      if (null != map) return map;
      Type[] ts = ((ParameterizedType) t).getActualTypeArguments();

      Object[] newvalues = new Object[values.length];
      for (int i = 0; i < keys.length; i++) {
         keys[i] = DBusConnection.deSerializeParameter(this.keys[i], ts[0]);
         newvalues[i] = DBusConnection.deSerializeParameter(this.values[i], ts[1]);
      }
      values = newvalues;
      this.map = new DBusMap<Object,Object>(keys,values);
      return map;
   }
}
