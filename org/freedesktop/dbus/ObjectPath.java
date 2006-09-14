package org.freedesktop.dbus;

class ObjectPath
{
   public String source;
   public String path;
   public DBusConnection conn;
   public ObjectPath(String source, String path, DBusConnection conn)
   {
      this.conn = conn;
      this.source = source;
      this.path = path;
   }
}
