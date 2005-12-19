package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;

import java.util.List;

public interface TestRemoteInterface2 extends DBusInterface
{
   public <A> TestTuple<String, Integer, Boolean> show(A in);
   public <T> T dostuff(TestStruct<String, UInt32, Variant<T>> foo);
   public List<Integer> sampleArray(List<String> l, Integer[] is, long[] ls);
   public DBusInterface getThis(DBusInterface t);
}
