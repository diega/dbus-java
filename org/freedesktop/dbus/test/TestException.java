package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusExecutionException;

public class TestException extends DBusExecutionException
{
   public TestException(String message)
   {
      super (message);
   }
}
