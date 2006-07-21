package org.freedesktop.dbus.test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusExecutionException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;

public class cross_test_client implements DBus.Binding.TestCallbacks, DBusSigHandler<DBus.Binding.TestSignals.Triggered>
{
   private DBusConnection conn;
   private static Set<String> passed = new TreeSet<String>();
   private static Map<String, List<String>> failed = new HashMap<String, List<String>>();
   private static cross_test_client ctc;
   static {
      List<String> l = new Vector<String>();
      l.add("Signal never arrived");
      failed.put("org.freedesktop.DBus.Binding.TestSignals.Triggered", l);
      l = new Vector<String>();
      l.add("Method never called");
      failed.put("org.freedesktop.DBus.Binding.TestCallbacks.Response", l);
   }
   public cross_test_client(DBusConnection conn)
   {
      this.conn = conn;
   }
   public boolean isRemote() { return false; }
   public void handle(DBus.Binding.TestSignals.Triggered t)
   {
      failed.remove("org.freedesktop.DBus.Binding.TestSignals.Triggered");
      if (t.a.equals(new UInt64(21389479283L)))
         pass("org.freedesktop.DBus.Binding.TestSignals.Triggered");
      else
         fail("org.freedesktop.DBus.Binding.TestSignals.Triggered", "Incorrect signal content; expected 21389479283 got "+t.a);
   }
   public void Response(UInt16 a, double b)
   {
      failed.remove("org.freedesktop.DBus.Binding.TestSignals.Triggered");
      if (a.equals(new UInt16(15)) && (b == 12.5))
         pass("org.freedesktop.DBus.Binding.TestSignals.Triggered");
      else
         fail("org.freedesktop.DBus.Binding.TestSignals.Triggered", "Incorrect signal content; expected 15, 12.5 got "+a+", "+b);
   }
   public static void pass(String test)
   {
      passed.add(test);
   }
   public static void fail(String test, String reason)
   {
      List<String> reasons = failed.get(test);
      if (null == reasons) {
         reasons = new Vector<String>();
         failed.put(test, reasons);
      }
      reasons.add(reason);
   }
   public static void test(Class iface, Object proxy, String method, Object rv, Object... parameters)
   {
      try {
         Method[] ms = iface.getMethods();
         Method m = null;
         for (Method t: ms) {
            if (t.getName().equals(method))
               m = t;
         }
            
         Object o = m.invoke(proxy, parameters);
         if (o == rv || (o != null && o.equals(rv)))
            pass(iface.getName()+"."+method);
         else
            fail(iface.getName()+"."+method, "Incorrect return value; expected "+rv+" got "+o);
      } catch (DBusExecutionException DBEe) {
         fail(iface.getName()+"."+method, "Error occurred during execution: "+DBEe.getClass().getName()+" "+DBEe.getMessage());
      } catch (InvocationTargetException ITe) {
         fail(iface.getName()+"."+method, "Error occurred during execution: "+ITe.getCause().getClass().getName()+" "+ITe.getCause().getMessage());
      } catch (Exception e) {
         fail(iface.getName()+"."+method, "Error occurred during execution: "+e.getClass().getName()+" "+e.getMessage());
      }
   }
   public static void doTests(DBus.Binding.Tests tests, DBus.Binding.SingleTests singletests)
   {
      test(DBus.Binding.Tests.class, tests, "IdentityBool", false, false); 
      test(DBus.Binding.Tests.class, tests, "IdentityBool", true, true); 
      test(DBus.Binding.Tests.class, tests, "Invert", false, true); 
      test(DBus.Binding.Tests.class, tests, "Invert", true, false); 
      test(DBus.Binding.Tests.class, tests, "IdentityByte", (byte) 0, (byte) 0); 
      test(DBus.Binding.Tests.class, tests, "IdentityByte", (byte) 1, (byte) 1); 
      test(DBus.Binding.Tests.class, tests, "IdentityByte", (byte) -1, (byte) -1); 
      test(DBus.Binding.Tests.class, tests, "IdentityByte", (byte) Byte.MAX_VALUE, (byte) Byte.MAX_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityByte", (byte) Byte.MIN_VALUE, (byte) Byte.MIN_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt16", (short) 0, (short) 0); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt16", (short) 1, (short) 1); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt16", (short) -1, (short) -1); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt16", (short) Short.MAX_VALUE, (short) Short.MAX_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt16", (short) Short.MIN_VALUE, (short) Short.MIN_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt32", (int) 0, (int) 0); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt32", (int) 1, (int) 1); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt32", (int) -1, (int) -1); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt32", (int) Integer.MAX_VALUE, (int) Integer.MAX_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt32", (int) Integer.MIN_VALUE, (int) Integer.MIN_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt64", (long) 0, (long) 0); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt64", (long) 1, (long) 1); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt64", (long) -1, (long) -1); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt64", (long) Long.MAX_VALUE, (long) Long.MAX_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityInt64", (long) Long.MIN_VALUE, (long) Long.MIN_VALUE); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt16", new UInt16(0), new UInt16(0)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt16", new UInt16(1), new UInt16(1)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt16", new UInt16(UInt16.MAX_VALUE), new UInt16(UInt16.MAX_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt16", new UInt16(UInt16.MIN_VALUE), new UInt16(UInt16.MIN_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt32", new UInt32(0), new UInt32(0)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt32", new UInt32(1), new UInt32(1)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt32", new UInt32(UInt32.MAX_VALUE), new UInt32(UInt32.MAX_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt32", new UInt32(UInt32.MIN_VALUE), new UInt32(UInt32.MIN_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt64", new UInt64(0), new UInt64(0)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt64", new UInt64(1), new UInt64(1)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt64", new UInt64(UInt64.MAX_VALUE), new UInt64(UInt64.MAX_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityUInt64", new UInt64(UInt64.MIN_VALUE), new UInt64(UInt64.MIN_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityDouble", new Double(0), new Double(0)); 
      test(DBus.Binding.Tests.class, tests, "IdentityDouble", new Double(1), new Double(1)); 
      test(DBus.Binding.Tests.class, tests, "IdentityDouble", new Double(-1), new Double(-1)); 
      test(DBus.Binding.Tests.class, tests, "IdentityDouble", new Double(Double.MAX_VALUE), new Double(Double.MAX_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityDouble", new Double(Double.MIN_VALUE), new Double(Double.MIN_VALUE)); 
      test(DBus.Binding.Tests.class, tests, "IdentityString", "", ""); 
      test(DBus.Binding.Tests.class, tests, "IdentityString", "The Quick Brown Fox Jumped Over The Lazy Dog", "The Quick Brown Fox Jumped Over The Lazy Dog"); 
      test(DBus.Binding.Tests.class, tests, "IdentityString", "ひらがなゲーム - かなぶん", "ひらがなゲーム - かなぶん"); 
      /*testArray(DBus.Binding.Tests.class, tests, "IdentityBoolArray", Boolean.TYPE);
      testArray(DBus.Binding.Tests.class, tests, "IdentityByteArray", Byte.TYPE);
      testArray(DBus.Binding.Tests.class, tests, "IdentityInt16Array", Short.TYPE);
      testArray(DBus.Binding.Tests.class, tests, "IdentityInt32Array", Integer.TYPE);
      testArray(DBus.Binding.Tests.class, tests, "IdentityInt64Array", Long.TYPE);
      testArray(DBus.Binding.Tests.class, tests, "IdentityUInt16Array", UInt16.class);
      testArray(DBus.Binding.Tests.class, tests, "IdentityUInt32Array", UInt32.class);
      testArray(DBus.Binding.Tests.class, tests, "IdentityUInt64Array", UInt64.class);
      testArray(DBus.Binding.Tests.class, tests, "IdentityDoubleArray", Double.class);
      testArray(DBus.Binding.Tests.class, tests, "IdentityStringArray", String.class);
      
      int[] is = new int[0];
      test(DBus.Binding.Tests.class, tests, "Sum", 0L, is); 
      Random r = new Random();
      int len = (r.nextInt() % 100) + 15;
      is = new int[(len<0 ? -len: len)+15];
      long result = 0;
      for (int i = 0; i < len; i++) {
         is[i] = r.nextInt();
         result += is[i];
      }
      test(DBus.Binding.Tests.class, tests, "Sum", result, is); 

      byte[] bs = new byte[0];
      test(DBus.Binding.SingleTests.class, singletests, "Sum", (short) 0, bs); 
      len = (r.nextInt() % 100);
      bs = new byte[(len<0 ? -len: len)+15];
      short res = 0;
      for (int i = 0; i < len; i++) {
         bs[i] = (byte) r.nextInt();
         res += bs[i];
      }
      test(DBus.Binding.SingleTests.class, singletests, "Sum", res, bs); 

      test(DBus.Binding.Tests.class, tests, "DeStruct", new DBus.Binding.Triplet<String,UInt32,Short>("hi", new UInt32(12), new Short((short) 99)), new DBus.Binding.TestStruct("hi", new UInt32(12), new Short((short) 99))); 

      Map<String, String> in = new HashMap<String, String>();
      Map<String, List<String>> out = new HashMap<String, List<String>>();
      test(DBus.Binding.Tests.class, tests, "InvertMapping", in, out);
      in.put("hi", "there");
      in.put("to", "there");
      in.put("from", "here");
      in.put("in", "out");
      List<String> l = new Vector<String>();
      l.add("hi");
      l.add("to");
      out.put("there", l);
      l = new Vector<String>();
      l.add("from");
      out.put("here", l);
      l = new Vector<String>();
      l.add("in");
      out.put("out", l);
      test(DBus.Binding.Tests.class, tests, "InvertMapping", in, out);

      test(DBus.Binding.Tests.class, tests, "Trigger", null, tests, new UInt64(21389479283L));

      try {
         ctc.conn.sendSignal(new DBus.Binding.TestSignals.Trigger("/Test", new UInt16(15), 12.5));
      } catch (DBusException DBe) {
         throw new DBusExecutionException(DBe.getMessage());
      }*/
         
      test(DBus.Binding.Tests.class, tests, "Exit", null);
   }
   public static void testArray(Class iface, Object proxy, String method, Class arrayType)
   {
      Object array = Array.newInstance(arrayType, 0);
      test(iface, proxy, method, array, array);
      Random r = new Random();
      int l = (r.nextInt() % 100);
      array = Array.newInstance(arrayType, (l < 0 ? -l : l) + 15);
      test(iface, proxy, method, array, array);
   }
         
   public static void main(String[] args) throws DBusException
   {
      /* init */
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      ctc = new cross_test_client(conn);
      conn.exportObject("/Test", ctc);
      conn.addSigHandler(DBus.Binding.TestSignals.Triggered.class, ctc);
      DBus.Binding.Tests tests = (DBus.Binding.Tests) conn.getRemoteObject("org.freedesktop.DBus.Binding.TestServer", "/Test", DBus.Binding.Tests.class);
      DBus.Binding.SingleTests singletests = (DBus.Binding.SingleTests) conn.getRemoteObject("org.freedesktop.DBus.Binding.TestServer", "/Test", DBus.Binding.SingleTests.class);

      doTests(tests, singletests);

      /* report results */
      for (String s: passed)
         System.out.println(s+" ok");
      int i = 1;
      for (String s: failed.keySet()) 
         for (String r: failed.get(s)) {
            System.out.println(s+" fail "+i);
            System.out.println("report "+i+": "+r);
            i++;
         }
      
      conn.disconnect();
   }
}
