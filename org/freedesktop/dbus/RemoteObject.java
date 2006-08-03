package org.freedesktop.dbus;

class RemoteObject
{
   String busname;
   String objectpath;
   Class<? extends DBusInterface> iface;
   public RemoteObject(String busname, String objectpath, Class<? extends DBusInterface> iface)
   {
      this.busname = busname;
      this.objectpath = objectpath;
      this.iface = iface;
   }
   public boolean equals(Object o)
   {
      if (!(o instanceof RemoteObject)) return false;
      RemoteObject them = (RemoteObject) o;
      if (!them.busname.equals(this.busname)) return false;
      if (!them.objectpath.equals(this.objectpath)) return false;
      if (!them.iface.equals(this.iface)) return false;
      return true;
   }
   public int hashCode()
   {
      return busname.hashCode() + objectpath.hashCode() + iface.hashCode();
   }
}
