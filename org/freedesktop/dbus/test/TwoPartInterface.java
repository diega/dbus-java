package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;

public interface TwoPartInterface extends DBusInterface
{
   public TwoPartObject getNew();
}
