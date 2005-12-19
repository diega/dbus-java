package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
/**
 * A sample signal with two parameters
 */
public interface TestSignalInterface extends DBusInterface
{
   public static class TestSignal<A extends String, B extends UInt32> extends DBusSignal
   {
      public final A value;
      public final B number;
      /**
       * Create a signal.
       */
      public TestSignal(String path, A value, B number) throws DBusException
      {
         super(path, value, number);
         this.value = value;
         this.number = number;
      }
   }
   public static class TestArraySignal<A extends TestStruct2<String[],Variant>> extends DBusSignal
   {
      public final A v;
      public TestArraySignal(String path, A v) throws DBusException
      {
         super(path, v);
         this.v = v;
      }
   }
}
