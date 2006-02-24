package org.freedesktop.dbus;

public class NoReply extends DBusExecutionException
{
   public NoReply(String message)
   {
      super (message);
   }
}
