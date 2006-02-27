package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Variant;

import java.util.List;

public final class TestStruct2 extends Struct
{
   @Position(0)
   public final List<String> a;
   @Position(1)
   public final Variant b;
   public TestStruct2(List<String> a, Variant b) throws DBusException
   {
      this.a = a;
      this.b = b;
   }
}
