package org.freedesktop.dbus;

/**
 * An exception within DBus.
 */
@SuppressWarnings("serial")
public class DBusException extends Exception
{
   /**
    * Create an exception with the specified message
    */
   public DBusException(String message)
   {
      super(message);
   }
}
