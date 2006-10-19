/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusConnection;

public class two_part_test_client
{
   public static void main(String[] args) throws Exception
   {
      System.out.println("get conn");
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      System.out.println("get remote");
      TwoPartInterface remote = (TwoPartInterface) conn.getRemoteObject("org.freedesktop.dbus.test.two_part_server", "/", TwoPartInterface.class);
      System.out.println("get object");
      TwoPartObject o = remote.getNew();
      System.out.println("get name");
      System.out.println(o.getName());
      conn.disconnect();
   }
}
