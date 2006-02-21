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
public interface TestRemoteInterface extends DBusInterface
{
   /**
    * A simple method with no parameters which returns a String
    */
   @Description("Simple test method")
   public String getName();
   @Description("Test of nested maps")
   public <T> int frobnicate(List<Long> n, Map<String,Map<UInt16,Short>> m, T v);
   @Description("Throws a TestException when called")
   public void throwme() throws TestException;
   @Description("Waits then doesn't return")
   @Method.NoReply()
   public void waitawhile();
}
