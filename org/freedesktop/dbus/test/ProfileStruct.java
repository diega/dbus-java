package org.freedesktop.dbus.test;

import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;

public final class ProfileStruct extends Struct
{
   @Position(0)
   public final String a;
   @Position(1)
   public final UInt32 b;
   @Position(2)
   public final long c;

   public ProfileStruct(String a, UInt32 b, long c)
   {
      this.a = a;
      this.b = b;
      this.c = c;
   }
}
