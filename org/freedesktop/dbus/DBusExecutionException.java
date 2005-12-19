package org.freedesktop.dbus;

/**
 * An exception while running a remote method within DBus.
 */
public class DBusExecutionException extends RuntimeException
{
   /**
    * Create an exception with the specified message
    */
   public DBusExecutionException(String message)
   {
      super(message);
   }
}
