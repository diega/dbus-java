package org.freedesktop.dbus;

/**
 * Holds information on a method call
 */
public class DBusCallInfo
{
   /**
    * Indicates the caller won't wait for a reply (and we won't send one).
    */
   public static final int NO_REPLY = MethodCall.NO_REPLY;
   private static final int ASYNC = MethodCall.ASYNC;
   private String source;
   private String destination;
   private String objectpath;
   private String iface;
   private String method;
   private int flags;
   DBusCallInfo(MethodCall m)
   {
      this.source = m.getSource();
      this.destination = m.getService();
      this.objectpath = m.getObjectPath();
      this.iface = m.getType();
      this.method = m.getName();
      this.flags = m.getFlags();
   }
  
   /** Returns the BusID which called the method */
   public String getSource() { return source; }
   /** Returns the name with which we were addressed on the Bus */
   public String getDestination() { return destination; }
   /** Returns the object path used to call this method */
   public String getObjectPath() { return objectpath; }
   /** Returns the interface this method was called with */
   public String getInterface() { return iface; }
   /** Returns the method name used to call this method */
   public String getMethod() { return method; }
   /** Returns any flags set on this method call */
   public int getFlags() { return flags; }
}
