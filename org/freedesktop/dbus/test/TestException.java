package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusExecutionException;
import org.freedesktop.DBus.Description;

@Description("A test exception to throw over DBus")
public class TestException extends DBusExecutionException
{
   public TestException(String message)
   {
      super (message);
   }
}
