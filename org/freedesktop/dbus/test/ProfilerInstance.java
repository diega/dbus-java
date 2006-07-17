package org.freedesktop.dbus.test;

import java.util.List;
import java.util.Map;

public class ProfilerInstance implements Profiler
{
   public boolean isRemote() { return false; }
   public void array(int[] v) { return; }
   public void map(Map<String,String> m) { return; }
   public void list(List<String> l) { return; }
   public void bytes(byte[] b) { return; }
   public void struct(ProfileStruct ps) { return; }
}
