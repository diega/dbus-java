/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.io.InputStream;
import java.io.IOException;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

import org.freedesktop.dbus.exceptions.MessageTypeException;
import org.freedesktop.dbus.exceptions.MessageProtocolVersionException;

public class MessageReader
{
   private InputStream in;
   public MessageReader(InputStream in)
   {
      this.in = in;
   }
   public Message readMessage() throws IOException
   {
      byte[] buf = new byte[12];
      in.read(buf);
      byte endian = buf[0];
      byte type = buf[1];
      byte protover = buf[3];
      if (protover > Message.PROTOCOL)
         throw new MessageProtocolVersionException("Protocol version "+protover+" is unsupported");
      int bodylen = (int) Message.demarshallint(buf, 4, endian, 4);
      byte[] tbuf = new byte[4];
      in.read(tbuf);
      int headerlen = (int) Message.demarshallint(tbuf, 0, endian, 4);
      if (0 != headerlen % 8)
         headerlen += 8-(headerlen%8);
      byte[] header=new byte[headerlen+8];
      byte[] body=new byte[bodylen];
      System.arraycopy(tbuf, 0, header, 0, 4);
      in.read(header, 8, headerlen);
      in.read(body);
      Message m;
      switch (type) {
         case Message.MessageType.METHOD_CALL:
            m = new MethodCall();
            break;
         case Message.MessageType.METHOD_RETURN:
            m = new MethodReturn();
            break;
         case Message.MessageType.SIGNAL:
            m = new DBusSignal();
            break;
         case Message.MessageType.ERROR:
            m = new Error();
            break;
         default:
            throw new MessageTypeException("Message type "+type+" unsupported");
      }
      m.populate(buf, header, body);
      return m;
   }
   public void close()
   {
      in.close();
   }
}
