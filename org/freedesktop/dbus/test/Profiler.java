package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import java.util.List;
import java.util.Map;

public interface Profiler extends DBusInterface
{
   public void array(int[] v);
   public void map(Map<String,String> m);
   public void list(List<String> l);
   public void bytes(byte[] b);
   public void struct(ProfileStruct ps);
}


