package org.freedesktop.Hal;

import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;

public interface Manager extends DBusInterface
{
   String[] GetAllDevices();
   boolean DeviceExists(String udi);
   String[] FindDeviceStringMatch(String key, String value);
   String[] FindDeviceByCapability(String capability);
   public class DeviceAdded<A extends String> extends DBusSignal
   {
      public final A udi;
      public DeviceAdded(String path, A udi) throws DBusException
      {
         super(path, udi);
         this.udi = udi;
      }
   }
   public class DeviceRemoved<A extends String> extends DBusSignal
   {
      public final A udi;
      public DeviceRemoved(String path, A udi) throws DBusException
      {
         super(path, udi);
         this.udi = udi;
      }
   }
   public class NewCapability<A extends String, B extends String> extends DBusSignal
   {
      public final A udi;
      public final B capability;
      public NewCapability(String path, A udi, B capability) throws DBusException
      {
         super(path, udi, capability);
         this.udi = udi;
         this.capability = capability;
      }
   }
}
