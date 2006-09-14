package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusConnection;

public class two_part_test_server implements TwoPartInterface
{
   public class two_part_test_object implements TwoPartObject
   {
      public boolean isRemote() { return false; }
      public String getName() 
      { 
         System.out.println("give name");
         return toString(); 
      }
   }
   private DBusConnection conn;
   public two_part_test_server(DBusConnection conn)
   {
      this.conn = conn;
   }
   public boolean isRemote() { return false; }
   public TwoPartObject getNew()
   {
      TwoPartObject o = new two_part_test_object();
      System.out.println("export new");
      try { conn.exportObject("/12345", o); } catch (Exception e) {}
      System.out.println("give new");
      return o;
   }
   public static void main(String[] args) throws Exception
   {
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      conn.requestBusName("org.freedesktop.dbus.test.two_part_server");
      conn.exportObject("/", new two_part_test_server(conn));
      while (true) try { Thread.sleep(10000); } catch (InterruptedException Ie) {}
   }
}

