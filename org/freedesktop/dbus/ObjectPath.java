/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

class ObjectPath
{
   public String source;
   public String path;
//   public DBusConnection conn;
   public ObjectPath(String source, String path/*, DBusConnection conn*/)
   {
      this.source = source;
      this.path = path;
  //    this.conn = conn;
   }
   public String toString() { return path; }
}
