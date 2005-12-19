package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.Struct;

public final class TestStruct2<A, B> extends Struct
{
   public final A a;
   public final B b;
   public TestStruct2(A a, B b) throws DBusException
   {
      super(a, b);
      this.a = a;
      this.b = b;
   }
}
