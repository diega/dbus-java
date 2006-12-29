/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

public class MessageWriter
{
   private OutputStream out;
   public MessageWriter(OutputStream out)
   {
      this.out = new BufferedOutputStream(out);
   }
   public void writeMessage(Message m) throws IOException
   {
      if (Debug.debug) {
         Debug.print(Debug.INFO, "<= "+m);
      }
      for (byte[] buf: m.getWireData()) {
         if (null == buf) break;
         if (Debug.debug)
            Debug.print(Debug.VERBOSE, "("+buf+"):"+ Hexdump.format(buf));
         out.write(buf);
      }
      out.flush();
   }
   public void close() throws IOException
   {
      out.close();
   }
}
