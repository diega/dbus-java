package org.freedesktop.dbus;

class MethodCall extends DBusMessage
{
   public static final int NO_REPLY = 1;
   public static final int ASYNC = 2;
   static long REPLY_WAIT_TIMEOUT = 20000;
   String service;
   String objectpath;
   DBusMessage reply = null;
   int flags = 0;
   public MethodCall(String service, String objectpath, String iface, String name, Object[] args)
   {
      super(null, iface, name, args, 0);
      this.service = service;
      this.objectpath = objectpath;
   }
   protected MethodCall(String source, String service, String objectpath, String iface, String name, Object[] args, long serial)
   {
      super(source, iface, name, args, serial);
      this.service = service;
      this.objectpath = objectpath;
   }
   public String getService() { return service; }
   public String getObjectPath() { return objectpath; }
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
