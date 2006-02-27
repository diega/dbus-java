package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;

public final class TestStruct extends Struct
{
   @Position(0)
   public final String a;
   @Position(1)
   public final UInt32 b;
   @Position(2)
   public final Variant c;
   public TestStruct(String a, UInt32 b, Variant c) throws DBusException
   {
      this.a = a;
      this.b = b;
      this.c = c;
   }
}
