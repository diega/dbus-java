package org.freedesktop.dbus.test;

import org.freedesktop.DBus;

public class cross_test_server implements DBus.Binding.Tests, DBus.Binding.SingleTests, DBusSigHandler<DBus.Binding.TestSignals.Trigger>;
{
   private DBusConnection conn;
   public cross_test_server(DBusConnection conn)
   {
      this.conn = conn;
   }
   @Description("Returns whatever it is passed")
   public Variant Identity(Variant input)
   {
      return new Variant(input.getValue());
   }
   @Description("Returns whatever it is passed")
   public byte IdentityByte(byte input) { return input; }
   @Description("Returns whatever it is passed")
   public boolean IdentityBool(boolean input) { return input; }
   @Description("Returns whatever it is passed")
   public short IdentityInt16(short input) { return input; }
   @Description("Returns whatever it is passed")
   public UInt16 IdentityUInt16(UInt16 input) { return input; }
   @Description("Returns whatever it is passed")
   public int IdentityInt32(int input) { return input; }
   @Description("Returns whatever it is passed")
   public UInt32 IdentityUInt32(UInt32 input) { return input; }
   @Description("Returns whatever it is passed")
   public long IdentityInt64(long input) { return input; }
   @Description("Returns whatever it is passed")
   public UInt64 IdentityUInt64(UInt64 input) { return input; }
   @Description("Returns whatever it is passed")
   public double IdentityDouble(double input) { return input; }
   @Description("Returns whatever it is passed")
   public String IdentityString(String input) { return input; }
   @Description("Returns whatever it is passed")
   public Variant[] IdentityArray(Variant[] input) { return input; }
   @Description("Returns whatever it is passed")
   public byte[] IdentityByteArray(byte[] input) { return input; }
   @Description("Returns whatever it is passed")
   public boolean[] IdentityBoolArray(boolean[] input) { return input; }
   @Description("Returns whatever it is passed")
   public short[] IdentityInt16Array(short[] input) { return input; }
   @Description("Returns whatever it is passed")
   public UInt16[] IdentityUInt16Array(UInt16[] input) { return input; }
   @Description("Returns whatever it is passed")
   public int[] IdentityInt32Array(int[] input) { return input; }
   @Description("Returns whatever it is passed")
   public UInt32[] IdentityUInt32Array(UInt32[] input) { return input; }
   @Description("Returns whatever it is passed")
   public long[] IdentityInt64Array(long[] input) { return input; }
   @Description("Returns whatever it is passed")
   public UInt64[] IdentityUInt64Array(UInt64[] input) { return input; }
   @Description("Returns whatever it is passed")
   public double[] IdentityDoubleArray(double[] input) { return input; }
   @Description("Returns whatever it is passed")
   public String[] IdentityStringArray(String[] input) { return input; }
   @Description("Returns the sum of the values in the input list")
   public long Sum(int[] a)
   {
      long sum = 0;
      for (int b: a) sum += b;
      return sum;
   }
   @Description("Returns the sum of the values in the input list")
   public UInt32 Sum(byte[] a)
   {
      int sum = 0;
      for (byte b: a) sum += b;
      return new UInt32(sum % (UInt32.MAX_VALUE+1));
   }
   @Description("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
   public Map<String, List<String>> InvertMapping(Map<String, String> a)
   {
      HashMap<String, List<String>> m = new HashMap<String, List<String>>();
      for (String s: a.keySet()) {
         String b = a.get(s);
         List<String> l = m.get(b);
         if (null == l) {
            l = new List<String>();
            m.put(b, l);
         }
         l.add(s);
      }
      return m;
   }
   @Description("This method returns the contents of a struct as separate values")
   public Triplet<String, UInt32, Short> DeStruct(TestStruct a)
   {
      return new Triplet<String, UInt32, Short>(a.a, a.b, a.c);
   }
   @Description("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
   public List<Variant> Primitize(Variant a)
   {
      List<Variant> vs = new List<Variant>();
      // TODO: do stuff
      return vs;
   }
   @Description("inverts it's input")
   public boolean Invert(boolean a)
   {
      return !a;
   }
   @Description("triggers sending of a signal from the supplied object with the given parameter")
   public void Trigger(DBusInterface a, UInt64 b)
   {
      conn.sendSignal(new DBus.Binding.TestSignals.Triggered(a, b));
   }
   public void Exit()
   {
      conn.disconnect();
      System.exit(0);
   }
   public void handle(DBus.Binding.TestSignals.Trigger t)
   {
      DBusCallInfo info = DBusConnection.getCallInfo();
      DBus.Binding.TestCallbacks cb = (DBus.Binding.TestCallbacks) conn.getRemoteObject(info.getSource(), "/Test", DBus.Binding.TestCallbacks.class);
      cb.Response(t.a, t.b);
   }

   public static void main(String[] args)
   {
      DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
      cross_test_server cts = new cross_test_server(conn);
      conn.addSigHandler(DBus.Binding.TestSignals.Trigger.class, cts);
      conn.exportObject("/Test", cts);
   }
}

