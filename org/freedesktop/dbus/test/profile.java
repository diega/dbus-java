package org.freedesktop.dbus.test;

import java.util.Random;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.DBus.Peer;
import org.freedesktop.DBus.Introspectable;

interface Profiler extends DBusInterface
{
   public void array(int[] v);
}

class ProfilerInstance implements Profiler
{
   public boolean isRemote() { return false; }
   public void array(int[] v) { return; }
}

/**
 * Profiling tests.
 */
public class profile
{
   public static void main(String[] args) throws DBusException
   {
      if (0==args.length) {
         System.out.println("You must specify a profile type.");
         System.out.println("Syntax: profile <pings|arrays|introspect>");
         System.exit(1);
      }
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      conn.registerService("org.freedesktop.DBus.java.profiler");
      if ("pings".equals(args[0])) {
         System.out.print("Sending 10000 pings...");
         Peer p = (Peer) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Peer.class);
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++)
               p.Ping();
            System.out.print(".");
         }
         System.out.println(" done in "+(System.currentTimeMillis()-t)+"ms.");
      } else if ("arrays".equals(args[0])) {
         System.out.print("Sending array of 100 ints 100 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         int[] v = new int[100];
         Random r = new Random();
         for (int i = 0; i < 100; i++) v[i] = r.nextInt();
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            p.array(v);
            System.out.print(".");
         }
         System.out.println(" done in "+(System.currentTimeMillis()-t)+"ms.");
      } else if ("introspect".equals(args[0])) {
         System.out.print("Recieving introspection data 1000 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Introspectable is = (Introspectable) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Introspectable.class);
         long t = System.currentTimeMillis();
         String s = null;
         for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++)
               s = is.Introspect();
            System.out.print(".");
         }
         System.out.println(" done in "+(System.currentTimeMillis()-t)+"ms.");
         System.out.println("Introspect data: "+s);

      } else {
         conn.disconnect();
         System.out.println("Invalid profile ``"+args[0]+"''.");
         System.out.println("Syntax: profile <pings|arrays|introspect>");
      }
      conn.disconnect();
   }
}
