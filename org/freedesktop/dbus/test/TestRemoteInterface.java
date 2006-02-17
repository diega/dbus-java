package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt16;

import java.util.Map;
import java.util.List;
/**
 * A sample remote interface which exports one method.
 */
public interface TestRemoteInterface extends DBusInterface
{
   /**
    * A simple method with no parameters which returns a String
    */
   public String getName();
   public <T> int frobnicate(List<Long> n, Map<String,Map<UInt16,Short>> m, T v);
   @org.freedesktop.DBus.Description("Throws a TestException when called");
   public void throwme() throws TestException;
}
