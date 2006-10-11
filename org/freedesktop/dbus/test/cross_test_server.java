package org.freedesktop.dbus.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;
import java.util.Vector;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusExecutionException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;

public class cross_test_server implements DBus.Binding.Tests, DBus.Binding.SingleTests, DBusSigHandler<DBus.Binding.TestSignals.Trigger>
{
   private DBusConnection conn;
   boolean run = true;
   private Set<String> done = new TreeSet<String>();
   private Set<String> notdone = new TreeSet<String>();
   {
      notdone.add("org.freedesktop.DBus.Binding.Tests.Identity");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityByte");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityBool");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityString");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityArray");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
      notdone.add("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
      notdone.add("org.freedesktop.DBus.Binding.Tests.Sum");
      notdone.add("org.freedesktop.DBus.Binding.SingleTests.Sum");
      notdone.add("org.freedesktop.DBus.Binding.Tests.InvertMapping");
      notdone.add("org.freedesktop.DBus.Binding.Tests.DeStruct");
      notdone.add("org.freedesktop.DBus.Binding.Tests.Primitize");
      notdone.add("org.freedesktop.DBus.Binding.Tests.Invert");
      notdone.add("org.freedesktop.DBus.Binding.Tests.Trigger");
      notdone.add("org.freedesktop.DBus.Binding.Tests.Exit");
      notdone.add("org.freedesktop.DBus.Binding.TestSignals.Trigger");
   }
   
   public cross_test_server(DBusConnection conn)
   {
      this.conn = conn;
   }
   public boolean isRemote() { return false; }
   @SuppressWarnings("unchecked")
   @DBus.Description("Returns whatever it is passed")
      public Variant Identity(Variant input)
      {
         done.add("org.freedesktop.DBus.Binding.Tests.Identity");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.Identity");
         return new Variant(input.getValue());
      }
   @DBus.Description("Returns whatever it is passed")
      public byte IdentityByte(byte input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityByte");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityByte");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public boolean IdentityBool(boolean input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityBool");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityBool");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public short IdentityInt16(short input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt16");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public UInt16 IdentityUInt16(UInt16 input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt16");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public int IdentityInt32(int input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt32");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public UInt32 IdentityUInt32(UInt32 input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt32");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public long IdentityInt64(long input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt64");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public UInt64 IdentityUInt64(UInt64 input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt64");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public double IdentityDouble(double input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityDouble");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public String IdentityString(String input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityString");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityString");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public Variant[] IdentityArray(Variant[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityArray");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityArray");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public byte[] IdentityByteArray(byte[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityByteArray");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public boolean[] IdentityBoolArray(boolean[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityBoolArray");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public short[] IdentityInt16Array(short[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt16Array");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public UInt16[] IdentityUInt16Array(UInt16[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt16Array");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public int[] IdentityInt32Array(int[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt32Array");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public UInt32[] IdentityUInt32Array(UInt32[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt32Array");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public long[] IdentityInt64Array(long[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityInt64Array");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public UInt64[] IdentityUInt64Array(UInt64[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityUInt64Array");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public double[] IdentityDoubleArray(double[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityDoubleArray");
         return input;
      }
   @DBus.Description("Returns whatever it is passed")
      public String[] IdentityStringArray(String[] input) 
      {
         done.add("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
         notdone.remove("org.freedesktop.DBus.Binding.Tests.IdentityStringArray");
         return input;
      }
   @DBus.Description("Returns the sum of the values in the input list")
   public long Sum(int[] a)
   {
      done.add("org.freedesktop.DBus.Binding.Tests.Sum");
      notdone.remove("org.freedesktop.DBus.Binding.Tests.Sum");
      long sum = 0;
      for (int b: a) sum += b;
      return sum;
   }
   @DBus.Description("Returns the sum of the values in the input list")
   public UInt32 Sum(byte[] a)
   {
      done.add("org.freedesktop.DBus.Binding.SingleTests.Sum");
      notdone.remove("org.freedesktop.DBus.Binding.SingleTests.Sum");
      int sum = 0;
      for (byte b: a) sum += b;
      sum = sum < 0 ? -sum : sum;
      return new UInt32(sum % (UInt32.MAX_VALUE+1));
   }
   @DBus.Description("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
   public Map<String, List<String>> InvertMapping(Map<String, String> a)
   {
      done.add("org.freedesktop.DBus.Binding.Tests.InvertMapping");
      notdone.remove("org.freedesktop.DBus.Binding.Tests.InvertMapping");
      HashMap<String, List<String>> m = new HashMap<String, List<String>>();
      for (String s: a.keySet()) {
         String b = a.get(s);
         List<String> l = m.get(b);
         if (null == l) {
            l = new Vector<String>();
            m.put(b, l);
         }
         l.add(s);
      }
      return m;
   }
   @DBus.Description("This method returns the contents of a struct as separate values")
   public DBus.Binding.Triplet<String, UInt32, Short> DeStruct(DBus.Binding.TestStruct a)
   {
      done.add("org.freedesktop.DBus.Binding.Tests.DeStruct");
      notdone.remove("org.freedesktop.DBus.Binding.Tests.DeStruct");
      return new DBus.Binding.Triplet<String, UInt32, Short>(a.a, a.b, a.c);
   }
   @DBus.Description("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
   @SuppressWarnings("unchecked")
   public List<Variant> Primitize(Variant a)
   {
      done.add("org.freedesktop.DBus.Binding.Tests.Primitize");
      notdone.remove("org.freedesktop.DBus.Binding.Tests.Primitize");
      List<Variant> vs = new Vector<Variant>();
     /* 
      // it's a list
      if (List.class.isAssignableFrom(a.getType())) {
         for (Object o: (List) a.getValue())
            vs.addAll(Primitize(new Variant(o)));
      
      // it's a map
      } else if (Map.class.isAssignableFrom(a.getType())) {
         for (Object o: ((Map) a.getValue()).keySet()) {
            vs.addAll(Primitize(new Variant(o)));
            vs.addAll(Primitize(new Variant(((Map)a.getValue()).get(o))));
         }
      
      // it's a struct
      } else if (Struct.class.isAssignableFrom(a.getType())) {
         try {
            for (Object o: ((Struct) a.getValue()).getParameters())
               vs.addAll(Primitize(new Variant(o)));
         } catch (DBusException DBe) {
            throw new DBusExecutionException(DBe.getMessage());
         }
      
      // it's already a primative in an variant, add it to the list
      } else { 
         vs.add(a); 
      }*/
      return vs;
   }
   @DBus.Description("inverts it's input")
   public boolean Invert(boolean a)
   {
      done.add("org.freedesktop.DBus.Binding.Tests.Invert");
      notdone.remove("org.freedesktop.DBus.Binding.Tests.Invert");
      return !a;
   }
   @DBus.Description("triggers sending of a signal from the supplied object with the given parameter")
   public void Trigger(String a, UInt64 b)
   {
      done.add("org.freedesktop.DBus.Binding.Tests.Trigger");
      notdone.remove("org.freedesktop.DBus.Binding.Tests.Trigger");
      try {
         conn.sendSignal(new DBus.Binding.TestSignals.Triggered(a, b));
      } catch (DBusException DBe) {
         throw new DBusExecutionException(DBe.getMessage());
      }
   }
   public void Exit()
   {
      done.add("org.freedesktop.DBus.Binding.Tests.Exit");
      notdone.remove("org.freedesktop.DBus.Binding.Tests.Exit");
      for (String s: done)
         System.out.println(s+" ok");
      for (String s: notdone)
         System.out.println(s+" untested");
      run = false;
      synchronized (this) {
         notifyAll();
      }
   }
   public void handle(DBus.Binding.TestSignals.Trigger t)
   {
      done.add("org.freedesktop.DBus.Binding.TestSignals.Trigger");
      notdone.remove("org.freedesktop.DBus.Binding.TestSignals.Trigger");
      try {
         DBus.Binding.TestCallbacks cb = (DBus.Binding.TestCallbacks) conn.getRemoteObject(t.getSource(), "/Test", DBus.Binding.TestCallbacks.class);
         cb.Response(t.a, t.b);
      } catch (DBusException DBe) {
         throw new DBusExecutionException(DBe.getMessage());
      }
   }

   public static void main(String[] args)
   { try {
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      conn.requestBusName("org.freedesktop.DBus.Binding.TestServer");
      cross_test_server cts = new cross_test_server(conn);
      conn.addSigHandler(DBus.Binding.TestSignals.Trigger.class, cts);
      conn.exportObject("/Test", cts);
      synchronized (cts) {
         while (cts.run) {
            try {
               cts.wait();
            } catch (InterruptedException Ie) {}
         }
      }
      conn.disconnect();
      System.exit(0);
   } catch (DBusException DBe) {
      DBe.printStackTrace();
      System.exit(1);
   }}
}

