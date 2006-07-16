package org.freedesktop.dbus.test;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.DBus.Peer;
import org.freedesktop.DBus.Introspectable;

/**
 * Profiling tests.
 */
public class profile
{
   public static void main(String[] args) throws DBusException
   {
      if (0==args.length) {
         System.out.println("You must specify a profile type.");
         System.out.println("Syntax: profile <pings|arrays|introspect|maps|bytes>");
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
         System.out.print("Sending array of 1000 ints 100 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         int[] v = new int[1000];
         Random r = new Random();
         for (int i = 0; i < 1000; i++) v[i] = r.nextInt();
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            p.array(v);
            System.out.print(".");
         }
         System.out.println(" done in "+(System.currentTimeMillis()-t)+"ms.");
      } else if ("maps".equals(args[0])) {
         System.out.print("Sending map of 100 string=>strings 1000 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         HashMap<String,String> m = new HashMap<String,String>();
         for (int i = 0; i < 100; i++) 
            m.put(""+i, "hello");
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j=0; j < 10; j++)
               p.map(m);
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
      } else if ("bytes".equals(args[0])) {
         System.out.print("Sending 5000000 bytes");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         byte[] bs = new byte[5000000];
         for (int i = 0; i < 5000000; i++) 
            bs[i] = (byte) i;            
         long t = System.currentTimeMillis();
         p.bytes(bs);
         System.out.println(" done in "+(System.currentTimeMillis()-t)+"ms.");
      } else {
         conn.disconnect();
         System.out.println("Invalid profile ``"+args[0]+"''.");
         System.out.println("Syntax: profile <pings|arrays|introspect|maps|bytes>");
         System.exit(1);
      }
      conn.disconnect();
   }
}
