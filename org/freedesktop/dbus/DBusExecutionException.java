package org.freedesktop.dbus;

/**
 * An exception while running a remote method within DBus.
 */
public class DBusExecutionException extends RuntimeException
{
   private String type;
   /**
    * Create an exception with the specified message
    */
   public DBusExecutionException(String message)
   {
      super(message);
   }
   void setType(String type)
   {
      this.type = type;
   }
   /**
    * Get the DBus type of this exception. Use if this
    * was an exception we don't have a class file for.
    */
   public String getType()
   {
      if (null == type) return getClass().getName();
      else return type;
   }
}
