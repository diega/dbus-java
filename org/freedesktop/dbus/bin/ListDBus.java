/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.bin;

import org.freedesktop.DBus;

/**
 * This class lists all the names currently connected on the bus
 */
public class ListDBus
{
   public static void syntax()
   {
      System.out.println("Syntax: ListDBus [--help] [-h] [--owners] [-o] [--uids] [-u] [--session] [-s] [--system] [-y]");
      System.exit(1);
   }
   public static void main(String args[]) throws Exception
   {
      boolean owners = false;
      boolean users = false;
      int connection = DBusConnection.SESSION;

      for (String a: args) 
         if ("--help".equals(a)) syntax();
         else if ("-h".equals(a)) syntax();
         else if ("-u".equals(a)) users = true;
         else if ("--uids".equals(a)) users = true;
         else if ("-o".equals(a)) owners = true;
         else if ("--owners".equals(a)) owners = true;
         else if ("--session".equals(a)) connection = DBusConnection.SESSION;
         else if ("-s".equals(a)) connection = DBusConnection.SESSION;
         else if ("--system".equals(a)) connection = DBusConnection.SYSTEM;
         else if ("-y".equals(a)) connection = DBusConnection.SYSTEM;
         else syntax();

      DBusConnection conn = DBusConnection.getConnection(connection);
      DBus dbus = (DBus) conn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
      String[] names = dbus.ListNames();
      for (String s: names) {
         if (users)
            try {
               System.out.print(dbus.GetConnectionUnixUser(s)+"\t");
            } catch (DBusExecutionException DBEe) {
               System.out.print("\t");
            }
         System.out.print(s);
         if (!s.startsWith(":") && owners) {
            try {
               System.out.print("\t"+dbus.GetNameOwner(s));
            } catch (DBusExecutionException DBEe) {
            }
         }
         System.out.println();
      }
      conn.disconnect();
   }
}
