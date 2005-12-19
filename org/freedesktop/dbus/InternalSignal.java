package org.freedesktop.dbus;
class InternalSignal extends DBusSignal
{
   public InternalSignal(String source, String objectpath, String name, String type, long serial, Object... parameters) throws DBusException
   {
      super(objectpath, parameters);
      this.type = type;
      this.name = name;
      this.serial = serial;
      this.source = source;
   }
}
