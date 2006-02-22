package org.freedesktop.dbus;

public class NotConnected extends DBusExecutionException
{
   public NotConnected(String message)
   {
      super (message);
   }
}
