/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.io.OutputStream;
import java.io.IOException;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

public class MessageWriter
{
   private OutputStream out;
   public MessageWriter(OutputStream out)
   {
      this.out = out;
   }
   public void writeMessage(Message m) throws IOException
   {
      if (DBusConnection.DBUS_JAVA_DEBUG && Debug.debug) {
         Debug.print(Debug.INFO, "<= "+m);
      }
      for (byte[] buf: m.getWireData()) {
         if (null == buf) break;
         Debug.print(Debug.VERBOSE, Hexdump.format(buf));
         out.write(buf);
      }
      out.flush();
   }
   public void close() throws IOException
   {
      out.close();
   }
}
