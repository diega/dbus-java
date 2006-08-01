package org.freedesktop.dbus.test;

import java.util.Random;
import java.util.HashMap;
import java.util.Vector;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.DBus.Peer;
import org.freedesktop.DBus.Introspectable;

class ProfileHandler implements DBusSigHandler<Profiler.ProfileSignal>
{
   public void handle(Profiler.ProfileSignal s)
   {
   }
}

/**
 * Profiling tests.
 */
public class profile
{
   public static class Log
   {
      private long last;
      private int[] deltas;
      private int current = 0;
      public Log(int size)
      {
         deltas = new int[size];
      }
      public void start()
      {
         last = System.currentTimeMillis();
      }
      public void stop()
      {
         deltas[current] = (int) (System.currentTimeMillis()-last);
         current++;
      }
      public double mean()
      {
         if (0 == current) return 0;
         long sum = 0;
         for (int i = 0; i < current; i++)
            sum+=deltas[i];
         return sum /= current;            
      }
      public long min()
      {
         int m = Integer.MAX_VALUE;
         for (int i = 0; i < current; i++)
            if (deltas[i] < m) m = deltas[i];
         return m;
      }
      public long max()
      {
         int m = 0;
         for (int i = 0; i < current; i++)
            if (deltas[i] > m) m = deltas[i];
         return m;
      }
      public double stddev()
      {
         double mean = mean();
         double sum = 0;
         for (int i=0; i < current; i++)
            sum += (deltas[i]-mean)*(deltas[i]-mean);
         return Math.sqrt(sum / (current-1));
      }
   }
   public static void main(String[] args) throws DBusException
   {
      if (0==args.length) {
         System.out.println("You must specify a profile type.");
         System.out.println("Syntax: profile <pings|arrays|introspect|maps|bytes|lists|structs|signals>");
         System.exit(1);
      }
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      conn.registerService("org.freedesktop.DBus.java.profiler");
      if ("pings".equals(args[0])) {
         System.out.print("Sending 10000 pings...");
         Peer p = (Peer) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Peer.class);
         Log l = new Log(10000);
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
               l.start();
               p.Ping();
               l.stop();
            }
            System.out.print(".");
         }
         t = System.currentTimeMillis()-t;
         System.out.println(" done.");
         System.out.println("min/max/avg (ms): "+l.min()+"/"+l.max()+"/"+l.mean());
         System.out.println("deviation: "+l.stddev());
         System.out.println("Total time: "+t+"ms");
      } else if ("arrays".equals(args[0])) {
         System.out.print("Sending array of 1000 ints 1000 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         int[] v = new int[1000];
         Random r = new Random();
         for (int i = 0; i < 1000; i++) v[i] = r.nextInt();
         Log l = new Log(1000);
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
               l.start();
               p.array(v);
               l.stop();
            }
            System.out.print(".");
         }
         t = System.currentTimeMillis()-t;
         System.out.println(" done.");
         System.out.println("min/max/avg (ms): "+l.min()+"/"+l.max()+"/"+l.mean());
         System.out.println("deviation: "+l.stddev());
         System.out.println("Total time: "+t+"ms");
      } else if ("maps".equals(args[0])) {
         System.out.print("Sending map of 100 string=>strings 1000 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         HashMap<String,String> m = new HashMap<String,String>();
         for (int i = 0; i < 100; i++) 
            m.put(""+i, "hello");
         Log l = new Log(1000);
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j=0; j < 10; j++) {
               l.start();
               p.map(m);
               l.stop();
            }
            System.out.print(".");
         }
         t = System.currentTimeMillis()-t;
         System.out.println(" done.");
         System.out.println("min/max/avg (ms): "+l.min()+"/"+l.max()+"/"+l.mean());
         System.out.println("deviation: "+l.stddev());
         System.out.println("Total time: "+t+"ms");
      } else if ("lists".equals(args[0])) {
         System.out.print("Sending list of 100 strings 1000 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         Vector<String> v = new Vector<String>();
         for (int i = 0; i < 100; i++) 
            v.add("hello "+i);
         Log l = new Log(1000);
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j=0; j < 10; j++) {
               l.start();
               p.list(v);
               l.stop();
            }
            System.out.print(".");
         }
         t = System.currentTimeMillis()-t;
         System.out.println(" done.");
         System.out.println("min/max/avg (ms): "+l.min()+"/"+l.max()+"/"+l.mean());
         System.out.println("deviation: "+l.stddev());
         System.out.println("Total time: "+t+"ms");
      } else if ("structs".equals(args[0])) {
         System.out.print("Sending a struct 1000 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Profiler p = (Profiler) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Profiler.class);
         ProfileStruct ps = new ProfileStruct("hello", new UInt32(18), 500L);
         Log l = new Log(1000);
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j=0; j < 10; j++) {
               l.start();
               p.struct(ps);
               l.stop();
            }
            System.out.print(".");
         }
         t = System.currentTimeMillis()-t;
         System.out.println(" done.");
         System.out.println("min/max/avg (ms): "+l.min()+"/"+l.max()+"/"+l.mean());
         System.out.println("deviation: "+l.stddev());
         System.out.println("Total time: "+t+"ms");
      } else if ("introspect".equals(args[0])) {
         System.out.print("Recieving introspection data 1000 times.");
         conn.exportObject("/Profiler", new ProfilerInstance());
         Introspectable is = (Introspectable) conn.getRemoteObject("org.freedesktop.DBus.java.profiler", "/Profiler", Introspectable.class);
         Log l = new Log(1000);
         long t = System.currentTimeMillis();
         String s = null;
         for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
               l.start();
               s = is.Introspect();
               l.stop();
            }
            System.out.print(".");
         }
         t = System.currentTimeMillis()-t;
         System.out.println(" done.");
         System.out.println("min/max/avg (ms): "+l.min()+"/"+l.max()+"/"+l.mean());
         System.out.println("deviation: "+l.stddev());
         System.out.println("Total time: "+t+"ms");
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
      } else if ("signals".equals(args[0])) {
         System.out.print("Sending 100000 signals");
         conn.addSigHandler(Profiler.ProfileSignal.class, new ProfileHandler());
         Log l = new Log(100000);
         Profiler.ProfileSignal p = new Profiler.ProfileSignal("/");
         long t = System.currentTimeMillis();
         for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 1000; j++) {
               l.start();
               conn.sendSignal(p);
               l.stop();
            }
            System.out.print(".");
         }
         t = System.currentTimeMillis()-t;
         System.out.println(" done.");
         System.out.println("min/max/avg (ms): "+l.min()+"/"+l.max()+"/"+l.mean());
         System.out.println("deviation: "+l.stddev());
         System.out.println("Total time: "+t+"ms");
      } else {
         conn.disconnect();
         System.out.println("Invalid profile ``"+args[0]+"''.");
         System.out.println("Syntax: profile <pings|arrays|introspect|maps|bytes|lists|structs|signals>");
         System.exit(1);
      }
      conn.disconnect();
   }
}
