package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Vector;
import java.util.List;
import java.util.Map;

class ListContainer
{
   private Object[] values;
   private String sig;
   private List list;
   public ListContainer(Object[] content, String sig) throws DBusException
   {
      Class c;
      try {
         String name = DBusConnection.getJavaType(sig, null, null, true, true);
         c = Class.forName(name);
         if (Map.class.isAssignableFrom(c)) 
            c = MapContainer.class;
         if (List.class.isAssignableFrom(c)) 
            c = ListContainer.class;
         this.values = (Object[]) Array.newInstance(c, content.length);
      } catch (ClassNotFoundException CNFe) {
         throw new DBusException("Map contains invalid type: "+CNFe.getMessage());
      }
      this.sig = sig;
      for (int i = 0; i < content.length; i++) {
         this.values[i] = content[i];
      }
   }
   public ListContainer(List l, ParameterizedType t) throws DBusException
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
         for (int i = 0; i < l.size(); i++) {
            values[i] = DBusConnection.convertParameters( new Object[] { l.get(i) }, new Type[] { ts[0] })[0];
         }
      } catch (Exception e) {
         throw new DBusException(e.getMessage());
      }

      String[] s = DBusConnection.getDBusType(ts[0]);
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

      this.list = new Vector();
      for (int i = 0; i < values.length; i++) {
         this.list.add(DBusConnection.deSerializeParameters(new Object[] { this.values[i] }, new Type[] { ts[0] })[0]);
      }
      return list; 
   }
}
