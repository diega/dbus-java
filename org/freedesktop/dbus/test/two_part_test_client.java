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
