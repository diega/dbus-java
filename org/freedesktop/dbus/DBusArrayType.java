/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
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
