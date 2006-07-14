package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import java.util.Map;

public interface Profiler extends DBusInterface
{
   public void array(int[] v);
   public void map(Map<String,String> m);
   public void bytes(byte[] b);
}


