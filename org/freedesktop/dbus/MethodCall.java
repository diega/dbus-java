/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

class MethodCall extends DBusMessage
{
   public static final int NO_REPLY = 1;
   public static final int ASYNC = 2;
   public static final int AUTO_START = 4;
   String destination;
   String objectpath;
   int flags = 0;
   public MethodCall(String busname, String objectpath, String iface, String name, Object[] args)
   {
      super(null, iface, name, "", args, 0);
      this.destination = busname;
      this.objectpath = objectpath;
   }
   protected MethodCall(String source, String busname, String objectpath, String iface, String name, String sig, Object[] args, long serial)
   {
      super(source, iface, name, sig, args, serial);
      this.destination = busname;
      this.objectpath = objectpath;
   }
   public String getDestination() { return destination; }
   public String getObjectPath() { return objectpath; }
   static long REPLY_WAIT_TIMEOUT = 20000;
   DBusMessage reply = null;
   public synchronized boolean hasReply()
   {
      return null != reply;
   }
   public synchronized DBusMessage getReply()
   {
      if (null != reply) return reply;
      try {
         wait(REPLY_WAIT_TIMEOUT);
         return reply;
      } catch (InterruptedException Ie) { return reply; }
   }
   protected synchronized void setReply(DBusMessage reply)
   {
      this.reply = reply;
      notifyAll();
   }
   void setFlags(int flags)
   {
      this.flags |= flags;
   }
   void clearFlags(int flags)
   {
      this.flags &= (~flags);
   }
   int getFlags()
   {
      return this.flags;
   }
}
