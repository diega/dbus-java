/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;
class InternalSignal extends DBusSignal
{
   public InternalSignal(String source, String objectpath, String name, String type, String sig, long serial, Object... parameters) throws DBusException
   {
      super(objectpath, parameters);
      this.type = type;
      this.name = name;
      this.serial = serial;
      this.source = source;
      this.sig = sig;
   }
}
