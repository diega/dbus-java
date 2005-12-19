package org.freedesktop.dbus.test;

import org.freedesktop.dbus.Tuple;

public final class TestTuple<A, B, C> extends Tuple
{
   public final A a;
   public final B b;
   public final C c;
   public TestTuple(A a, B b, C c)
   {
      super(a, b, c);
      this.a = a;
      this.b = b;
      this.c = c;
   }
}
