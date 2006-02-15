package org.freedesktop.dbus.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusExecutionException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;

import org.freedesktop.DBus.Error.UnknownObject;
import org.freedesktop.DBus.Error.ServiceUnknown;
import org.freedesktop.DBus.Peer;
import org.freedesktop.DBus.Introspectable;

class testclass implements TestRemoteInterface, TestRemoteInterface2, TestSignalInterface
{
   public <A> TestTuple<String, Integer, Boolean> show(A in)
   {
      System.out.println("Showing Stuff: "+in.getClass()+"("+in+")");
      if (!(in instanceof Integer) || ((Integer) in).intValue() != 234)
         test.fail("show received the wrong arguments");
      return new TestTuple("hi", 28165, true);
   }
   public <T> T dostuff(TestStruct<String, UInt32, Variant<T>> foo)
   {
      System.out.println("Doing Stuff "+foo);
      if (!(foo instanceof TestStruct) ||
            !(foo.a instanceof String) ||
            !(foo.b instanceof UInt32) ||
            !(foo.c instanceof Variant) ||
            !"bar".equals(foo.a) ||
            foo.b.intValue() != 52 ||
            !(foo.c.getValue() instanceof Boolean) ||
            ((Boolean) foo.c.getValue()).booleanValue() != true)
         test.fail("dostuff received the wrong arguments");
      return foo.c.getValue();
   }
   /** Local classes MUST implement this to return false */
   public boolean isRemote() { return false; }
   /** The method we are exporting to the Bus. */
   public List<Integer> sampleArray(List<String> ss, Integer[] is, long[] ls)
   {
      System.out.println("Got an array:");
      for (String s: ss)
         System.out.println("--"+s);
      if (ss.size()!= 5 ||
            !"hi".equals(ss.get(0)) ||
            !"hello".equals(ss.get(1)) ||
            !"hej".equals(ss.get(2)) ||
            !"hey".equals(ss.get(3)) ||
            !"aloha".equals(ss.get(4)))
         test.fail("sampleArray, String array contents incorrect");
      System.out.println("Got an array:");
      for (Integer i: is)
         System.out.println("--"+i);
      if (is.length != 4 ||
            is[0].intValue() != 1 ||
            is[1].intValue() != 5 ||
            is[2].intValue() != 7 ||
            is[3].intValue() != 9)
         test.fail("sampleArray, Integer array contents incorrect");
      System.out.println("Got an array:");
      for (long l: ls)
         System.out.println("--"+l);
      if (ls.length != 4 ||
            ls[0] != 2 ||
            ls[1] != 6 ||
            ls[2] != 8 ||
            ls[3] != 12)
         test.fail("sampleArray, Integer array contents incorrect");
      Vector<Integer> v = new Vector<Integer>();
      v.add(-1);
      v.add(-5);
      v.add(-7);
      v.add(-12);
      v.add(-18);
      return v;
   }
   public String getName()
   {
      return "This Is A Name!!";
   }
   public <T> int frobnicate(List<Long> n, Map<String,Map<UInt16,Short>> m, T v)
   {
      if (null == n)
         test.fail("List was null");
      if (n.size() != 3)
         test.fail("List was wrong size");
      if (n.get(0) != 2L ||
          n.get(1) != 5L ||
          n.get(2) != 71L)
         test.fail("List has wrong contents");
      if (!(v instanceof Integer))
         test.fail("v not an Integer");
      if (((Integer) v) != 13)
         test.fail("v is incorrect");
      if (null == m)
         test.fail("Map was null");
      if (m.size() != 1)
         test.fail("Map was wrong size");
      if (!m.keySet().contains("stuff"))
         test.fail("Incorrect key");
      Map<UInt16,Short> mus = m.get("stuff");
      if (null == mus)
         test.fail("Sub-Map was null");
      if (mus.size() != 3)
         test.fail("Sub-Map was wrong size");
      if (!(new Short((short)5).equals(mus.get(new UInt16(4)))))
         test.fail("Sub-Map has wrong contents");
      if (!(new Short((short)6).equals(mus.get(new UInt16(5)))))
         test.fail("Sub-Map has wrong contents");
      if (!(new Short((short)7).equals(mus.get(new UInt16(6)))))
         test.fail("Sub-Map has wrong contents");
      return -5;
   }
   public DBusInterface getThis(DBusInterface t)
   {
      if (!t.equals(this))
         test.fail("Didn't get this properly");
      return this;
   }
   public void throwme() throws TestException
   {
      throw new TestException("test");
   }
}

/**
 * This is a test program which sends and recieves a signal, implements, exports and calls a remote method.
 */
public class test implements DBusSigHandler
{
   public static void fail(String message)
   {
      System.err.println("Test Failed: "+message);
      if (null != conn) conn.disconnect();
      System.exit(1);
   }
   /** Handling a signal */
   public void handle(DBusSignal s)
   {
      System.out.println("SignalHandler Running");

      /** Known signals which we have a class for will be instantiated to that class. */
      if (s instanceof TestSignalInterface.TestSignal) {
         System.out.print("Got a test signal with Parameters: ");
         /** Use the test signal interface to get the parameters. */
         TestSignalInterface.TestSignal t = (TestSignalInterface.TestSignal) s;
         System.out.println("string("+t.value+") int("+t.number+")");
         if (!"Bar".equals(t.value) || !(new UInt32(42)).equals(t.number))
            fail("Incorrect TestSignal parameters");
      } 
      /** Known signals which we have a class for will be instantiated to that class. */
      else if (s instanceof TestSignalInterface.TestArraySignal) {
         System.out.println("Got a test array signal with Parameters: ");
         /** Use the test signal interface to get the parameters. */
         TestSignalInterface.TestArraySignal t = (TestSignalInterface.TestArraySignal) s;
         for (String str: t.v.a)
            System.out.println("--"+str);
         System.out.println(t.v.b.getType());
         System.out.println(t.v.b.getValue());
         if (!(t.v.b.getValue() instanceof UInt64) ||
             567L != ((UInt64) t.v.b.getValue()).longValue() ||
             t.v.a.length != 5 ||
             !"hi".equals(t.v.a[0]) ||
             !"hello".equals(t.v.a[1]) ||
             !"hej".equals(t.v.a[2]) ||
             !"hey".equals(t.v.a[3]) ||
             !"aloha".equals(t.v.a[4]))
            fail("Incorrect TestArraySignal parameters");
      } 
      /** Other signals. (should never happen, we are registered for only TestSignals) */
      else {
         System.out.println("Got an unknown signal: "+s);
         /** Use the generic signal interface to get the details. */
         System.out.println("Class: "+s.getClass());
         System.out.println("Type: "+s.getType());
         System.out.println("Name: "+s.getName());
         System.out.print("Parameters:");
         for (Object o: s.getParameters())
            System.out.print(" "+o);
         System.out.println();
         fail("Incorrect Signal Type");
      }
   }
   static DBusConnection conn = null;
   public static void main(String[] args) 
   { try {
      System.out.println("Creating Connection");
      conn = DBusConnection.getConnection(DBusConnection.SESSION);
      
      System.out.println("Registering Name");
      conn.registerService("foo.bar.Test");
      
      System.out.print("Listening for signals...");
      try {
         /** This registers an instance of the test class as the signal handler for the TestSignal class. */
         test t = new test();
         conn.addSigHandler(TestSignalInterface.TestSignal.class, t);
         conn.addSigHandler(TestSignalInterface.TestArraySignal.class, t);
         System.out.println("done");
      } catch (DBusException DBe) {
         test.fail("Failed to add handlers");
      }
      
      System.out.println("Listening for Method Calls");
      testclass tclass = new testclass();
      /** This exports an instance of the test class as the object /Test. */
      conn.exportObject("/Test", tclass);
      
      System.out.println("Sending Signal");
      /** This creates an instance of the Test Signal, with the given object path, signal name and parameters, and broadcasts in on the Bus. */
      conn.sendSignal(new TestSignalInterface.TestSignal("/foo/bar/com/Wibble", "Bar", new UInt32(42)));
      
      System.out.println("Getting our introspection data");
      /** This gets a remote object matching our service name and exported object path. */
      Introspectable intro = (Introspectable) conn.getRemoteObject("foo.bar.Test", "/", Introspectable.class);
      /** Get introspection data */
      String data = intro.Introspect();
      if (null == data || !data.startsWith("<!DOCTYPE"))
         fail("Introspection data invalid");
      System.out.println("Got Introspection Data: \n"+data);
      
      System.out.println("Pinging ourselves");
      /** This gets a remote object matching our service name and exported object path. */
      Peer peer = (Peer) conn.getRemoteObject("foo.bar.Test", "/Test", Peer.class);
      /** Call ping. */
      peer.Ping();
      
      System.out.println("Calling Method0/1");
      /** This gets a remote object matching our service name and exported object path. */
      TestRemoteInterface tri = (TestRemoteInterface) conn.getRemoteObject("foo.bar.Test", "/Test", TestRemoteInterface.class);
      System.out.println("Got Remote Object: "+tri);
      /** Call the remote object and get a response. */
      String rname = tri.getName();
      System.out.println("Got Remote Name: "+rname);
      if (!"This Is A Name!!".equals(rname))
         fail("getName return value incorrect");
      System.out.println("frobnicating");
      List<Long> ls = new Vector<Long>();
      ls.add(2L);
      ls.add(5L);
      ls.add(71L);
      Map<UInt16,Short> mus = new HashMap<UInt16,Short>();
      mus.put(new UInt16(4), (short) 5);
      mus.put(new UInt16(5), (short) 6);
      mus.put(new UInt16(6), (short) 7);
      Map<String,Map<UInt16,Short>> msmus = new HashMap<String,Map<UInt16,Short>>();
      msmus.put("stuff", mus);
      int rint = tri.frobnicate(ls, msmus, 13);
      if (-5 != rint)
         fail("frobnicate return value incorrect");
 
      /** call something that throws */
      try {
         System.out.println("Throwing stuff");
         tri.throwme();
         test.fail("Method Execution should have failed");
      } catch (TestException Te) {
         System.out.println("Remote Method Failed with: "+Te.getClass().getName()+" "+Te.getMessage());
         if (!Te.getMessage().equals("test"))
            test.fail("Error message was not correct");
      }
     
      /** Try and call an invalid remote object */
      try {
         System.out.println("Calling Method2");
         tri = (TestRemoteInterface) conn.getRemoteObject("foo.bar.NotATest", "/Moofle", TestRemoteInterface.class);
         System.out.println("Got Remote Name: "+tri.getName());
         test.fail("Method Execution should have failed");
      } catch (ServiceUnknown SU) {
         System.out.println("Remote Method Failed with: "+SU.getClass().getName()+" "+SU.getMessage());
         if (!SU.getMessage().equals("The name foo.bar.NotATest was not provided by any .service files"))
            test.fail("Error message was not correct");
      }
      
      /** Try and call an invalid remote object */
      try {
         System.out.println("Calling Method3");
         tri = (TestRemoteInterface) conn.getRemoteObject("foo.bar.Test", "/Moofle", TestRemoteInterface.class);
         System.out.println("Got Remote Name: "+tri.getName());
         test.fail("Method Execution should have failed");
      } catch (UnknownObject UO) {
         System.out.println("Remote Method Failed with: "+UO.getClass().getName()+" "+UO.getMessage());
         if (!UO.getMessage().equals("/Moofle is not an object provided by this service."))
            test.fail("Error message was not correct");
      }

      System.out.println("Calling Method4/5/6/7");
      /** This gets a remote object matching our service name and exported object path. */
      TestRemoteInterface2 tri2 = (TestRemoteInterface2) conn.getRemoteObject("foo.bar.Test", "/Test", TestRemoteInterface2.class);
      /** Call the remote object and get a response. */
      TestTuple<String, Integer, Boolean> rv = tri2.show(234);
      System.out.println("Show Response = "+rv);
      if (!"hi".equals(rv.a) ||
            28165 != rv.b.intValue() ||
            true != rv.c.booleanValue())
         fail("show return value incorrect");

      
      Boolean b = (Boolean) tri2.dostuff(new TestStruct("bar", new UInt32(52), new Variant<Boolean>(new Boolean(true))));
      System.out.println("Do stuff replied "+b);
      if (true != b.booleanValue())
         fail("dostuff return value incorrect");
      
      List<String> l = new Vector<String>();
      l.add("hi");
      l.add("hello");
      l.add("hej");
      l.add("hey");
      l.add("aloha");
      System.out.println("Sampling Arrays:");
      List<Integer> is = tri2.sampleArray(l, new Integer[] { 1, 5, 7, 9 }, new long[] { 2, 6, 8, 12 });
      System.out.println("sampleArray returned an array:");
      for (Integer i: is)
         System.out.println("--"+i);
      if (is.size() != 5 ||
            is.get(0).intValue() != -1 ||
            is.get(1).intValue() != -5 ||
            is.get(2).intValue() != -7 ||
            is.get(3).intValue() != -12 ||
            is.get(4).intValue() != -18)
         fail("sampleArray return value incorrect");

      System.out.println("Get This");
      if (!tclass.equals(tri2.getThis(tri2)))
         fail("Didn't get the correct this");
            
      
      System.out.print("Sending Array Signal...");
      /** This creates an instance of the Test Signal, with the given object path, signal name and parameters, and broadcasts in on the Bus. */
      conn.sendSignal(new TestSignalInterface.TestArraySignal("/foo/bar/com/Wibble", new TestStruct2(l.toArray(new String[0]), new Variant(new UInt64(567)))));
      
      System.out.println("done");

      /** Pause while we wait for the DBus messages to go back and forth. */
      Thread.sleep(1000);
    
      System.out.println("Disconnecting");
      /** Disconnect from the bus. */
      conn.disconnect();
      conn = null;
   } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected Exception Occurred");
   }}
}
