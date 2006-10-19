/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
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
   public class DeviceAdded extends DBusSignal
   {
      public final String udi;
      public DeviceAdded(String path, String udi) throws DBusException
      {
         super(path, udi);
         this.udi = udi;
      }
   }
   public class DeviceRemoved extends DBusSignal
   {
      public final String udi;
      public DeviceRemoved(String path, String udi) throws DBusException
      {
         super(path, udi);
         this.udi = udi;
      }
   }
   public class NewCapability extends DBusSignal
   {
      public final String udi;
      public final String capability;
      public NewCapability(String path, String udi, String capability) throws DBusException
      {
         super(path, udi, capability);
         this.udi = udi;
         this.capability = capability;
      }
   }
}
