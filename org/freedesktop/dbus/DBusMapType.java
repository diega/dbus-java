package org.freedesktop.dbus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

class DBusMapType implements ParameterizedType
{
   Type k;
   Type v;
   public DBusMapType(Type k, Type v)
   {
      this.k = k;
      this.v = v;
   }
   public Type[] getActualTypeArguments()
   {
      return new Type[] { k, v };
   }
   public Type getRawType()
   {
      return Map.class;
   }
   public Type getOwnerType()
   {
      return null;
   }
}
