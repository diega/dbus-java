package org.freedesktop.dbus;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

class DBusArrayType implements GenericArrayType
{
   Type v;
   public DBusArrayType(Type v)
   {
      this.v = v;
   }
   public Type getGenericComponentType()
   {
      return v;
   }
}
