package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.Struct;

public final class TestStruct<A, B, C> extends Struct
{
   public final A a;
   public final B b;
   public final C c;
   public TestStruct(A a, B b, C c) throws DBusException
   {
      super(a, b, c);
      this.a = a;
      this.b = b;
      this.c = c;
   }
}
