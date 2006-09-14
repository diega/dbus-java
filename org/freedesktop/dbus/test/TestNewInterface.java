package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.DBus.Description;
import org.freedesktop.DBus.Method;

import java.util.Map;
import java.util.List;
/**
 * A sample remote interface which exports one method.
 */
public interface TestNewInterface extends DBusInterface
{
   /**
    * A simple method with no parameters which returns a String
    */
   @Description("Simple test method")
   public String getName();
}
