/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.lang.reflect.Type;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DirectConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt16;

public class test_p2p_server implements TestRemoteInterface
{
   public String getName()
   {
      System.out.println("getName called");
      return "Peer2Peer Server";
   }
   public <T> int frobnicate(List<Long> n, Map<String,Map<UInt16,Short>> m, T v)
   {
      return 3;
   }
   public void throwme() throws TestException
   {
      System.out.println("throwme called");
      throw new TestException("BOO");
   }
   public void waitawhile()
   {
      return;
   }
   public int overload()
   {
      return 1;
   }
   public void sig(Type[] s)
   {
   }
   public void newpathtest(Path p)
   {
   }
   public boolean isRemote() { return false; }
   public float testfloat(float[] f)
   {
      System.out.println("got float: "+Arrays.toString(f));
      return f[0];
   }

   public static void main(String[] args) throws Exception
   {
      String address = DirectConnection.createDynamicSession();
      PrintWriter w = new PrintWriter(new FileOutputStream("address"));
      w.println(address);
      w.flush();
      w.close();
      DirectConnection dc = new DirectConnection(address+",listen=true");
      System.out.println("Connected");
      dc.exportObject("/Test", new test_p2p_server());
   }      
}
