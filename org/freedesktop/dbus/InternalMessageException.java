package org.freedesktop.dbus;

@SuppressWarnings("serial")
public class InternalMessageException extends DBusExecutionException
{
   public InternalMessageException(String message)
   {
      super (message);
   }
}
