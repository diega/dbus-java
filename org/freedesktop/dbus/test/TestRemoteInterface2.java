package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.DBus.Description;

import java.util.List;

@Description("An example remote interface")
public interface TestRemoteInterface2 extends DBusInterface
{
   @Description("Test multiple return values and implicit variant parameters.")
   public <A> TestTuple<String, List<Integer>, Boolean> show(A in);
   @Description("Test passing structs and explicit variants, returning implicit variants")
   public <T> T dostuff(TestStruct foo);
   @Description("Test arrays, boxed arrays and lists.")
   public List<Integer> sampleArray(List<String> l, Integer[] is, long[] ls);
   @Description("Test passing objects as object paths.")
   public DBusInterface getThis(DBusInterface t);
   @Description("Test bools work")
   public boolean check();
   @Description("Test Serializable Object")
   public void testSerializable(byte b, TestSerializable s, int i);
   @Description("Call another method on itself from within a call")
   public String recursionTest();
   @Description("Parameter-overloaded method (string)")
   public int overload(String s);
   @Description("Parameter-overloaded method (byte)")
   public int overload(byte b);
   @Description("Parameter-overloaded method (void)")
   public int overload();
   @Description("Nested List Check")
   public List<List<Integer>> checklist(List<List<Integer>> lli);
}
