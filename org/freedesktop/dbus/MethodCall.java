/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.util.Vector;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

public class MethodCall extends Message
{
   MethodCall() { }
   public MethodCall(String dest, String path, String iface, String member, byte flags, String sig, Object... args) throws DBusException
   {
      this(null, dest, path, iface, member, flags, sig, args);
   }
   public MethodCall(String source, String dest, String path, String iface, String member, byte flags, String sig, Object... args) throws DBusException
   {
      super(Message.Endian.BIG, Message.MessageType.METHOD_CALL, flags);

      if (null == member || null == path)
         throw new MessageFormatException("Must specify destination, path and function name to MethodCalls.");
      headers.put(Message.HeaderField.PATH,path);
      headers.put(Message.HeaderField.MEMBER,member);

      Vector<Object> hargs = new Vector<Object>();

      hargs.add(new Object[] { Message.HeaderField.PATH, new Object[] { ArgumentType.OBJECT_PATH_STRING, path } });
      
      if (null != source) {
         headers.put(Message.HeaderField.SENDER,source);
         hargs.add(new Object[] { Message.HeaderField.SENDER, new Object[] { ArgumentType.STRING_STRING, source } });
      }
      
      if (null != dest) {
         headers.put(Message.HeaderField.DESTINATION,dest);
         hargs.add(new Object[] { Message.HeaderField.DESTINATION, new Object[] { ArgumentType.STRING_STRING, dest } });
      }
      
      if (null != iface) {
         hargs.add(new Object[] { Message.HeaderField.INTERFACE, new Object[] { ArgumentType.STRING_STRING, iface } });
         headers.put(Message.HeaderField.INTERFACE,iface);
      }
      
      hargs.add(new Object[] { Message.HeaderField.MEMBER, new Object[] { ArgumentType. STRING_STRING, member } });

      if (null != sig) {
         if (Debug.debug) Debug.print(Debug.DEBUG, "Appending arguments with signature: "+sig);
         hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
         headers.put(Message.HeaderField.SIGNATURE,sig);
         setArgs(args);
      }

      byte[] blen = new byte[4];
      appendBytes(blen);
      append("ua(yv)", serial, hargs.toArray());
      pad((byte)8);

      long c = bytecounter;
      if (null != sig) append(sig, args);
      if (Debug.debug) Debug.print(Debug.DEBUG, "Appended body, type: "+sig+" start: "+c+" end: "+bytecounter+" size: "+(bytecounter-c));
      marshallint(bytecounter-c, blen, 0, 4);
      if (Debug.debug) Debug.print("marshalled size ("+blen+"): "+Hexdump.format(blen));
   }
   static long REPLY_WAIT_TIMEOUT = 20000;
   Message reply = null;
   public synchronized boolean hasReply()
   {
      return null != reply;
   }
   public synchronized Message getReply()
   {
      if (Debug.debug) Debug.print(Debug.VERBOSE, "Blocking on "+this);
      if (null != reply) return reply;
      try {
         wait(REPLY_WAIT_TIMEOUT);
         return reply;
      } catch (InterruptedException Ie) { return reply; }
   }
   protected synchronized void setReply(Message reply)
   {
      if (Debug.debug) Debug.print(Debug.VERBOSE, "Setting reply to "+this+" to "+reply);
      this.reply = reply;
      notifyAll();
   }

}
