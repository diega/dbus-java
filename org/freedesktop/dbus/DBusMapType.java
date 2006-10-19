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
