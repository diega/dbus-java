package org.freedesktop.dbus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class DBusStructType implements ParameterizedType
{
   Type[] contents;
   public DBusStructType(Type[] contents)
   {
      this.contents = contents;
   }
   public Type[] getActualTypeArguments()
   {
      return contents;
   }
   public Type getRawType()
   {
      return Struct.class;
   }
   public Type getOwnerType()
   {
      return null;
   }
}
