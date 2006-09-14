package org.freedesktop.dbus;

class RemoteObject
{
   String busname;
   String objectpath;
   Class<? extends DBusInterface> iface;
   boolean autostart;
   public RemoteObject(String busname, String objectpath, Class<? extends DBusInterface> iface, boolean autostart)
   {
      this.busname = busname;
      this.objectpath = objectpath;
      this.iface = iface;
      this.autostart = autostart;
   }
   public boolean equals(Object o)
   {
      if (!(o instanceof RemoteObject)) return false;
      RemoteObject them = (RemoteObject) o;
      if (!them.busname.equals(this.busname)) return false;
      if (!them.objectpath.equals(this.objectpath)) return false;
      if (null != this.iface && !them.iface.equals(this.iface)) return false;
      return true;
   }
   public int hashCode()
   {
      return busname.hashCode() + objectpath.hashCode() +
         (null == iface ? 0 : iface.hashCode());
   }
   public boolean autoStarting() { return autostart; }
   public String getBusName() { return busname; }
   public String getObjectPath() { return objectpath; }
   public Class<? extends DBusInterface>  getInterface() { return iface; }
   public String toString()
   {
      return busname+":"+objectpath+":"+iface;
   }
}
