package org.freedesktop.dbus;

class MethodCall extends DBusMessage
{
   static long REPLY_WAIT_TIMEOUT = 20000;
   String service;
   String objectpath;
   DBusMessage reply = null;
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
}
