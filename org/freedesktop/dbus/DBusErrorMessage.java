package org.freedesktop.dbus;

import java.lang.reflect.Constructor;

/**
 * Represents an error message sent over the Bus.
 * Any errors not associated with a method call are queued by the Bus 
 * and may be retrieved by calling DBusConnection.getError().
 * @see DBusConnection#getError()
 */
class DBusErrorMessage extends DBusMessage
{
   /** The Destination. */
   protected String destination;
   /**
    * Create an error message.
    * @param source The source address.
    * @param destination The destination address.
    * @param name The error name (the type of the error in dot-notation).
    * @param parameters The error parameters (usually a String message).
    * @param serial The serial of the message.
    * @param replyserial The serial of the message this is a reply to.
    */
   DBusErrorMessage(String source, String destination, String name, String sig, Object[] parameters, long serial, long replyserial)
   {
      super(source, name, name, sig, parameters, serial, replyserial);
      this.destination = destination;
   }
   /**
    * Create an error message.
    * @param m The message this is a reply to.
    * @param ex Exception that caused this message
    */
   protected DBusErrorMessage(DBusMessage m, DBusExecutionException ex)
   {
      super(null, null, null, "s", new Object[] { ex.getMessage() }, 0, m.getSerial());
      this.type = DBusConnection.dollar_pattern.matcher(ex.getClass().getName()).replaceAll(".");
      this.name = DBusConnection.dollar_pattern.matcher(ex.getClass().getName()).replaceAll(".");
      this.destination = m.getSource();
   }
   /**
    * Create an error message.
    * @param m The message this is a reply to.
    * @param parameters The error parameters (usually a String message).
    * @param ex Exception that caused this message
    */
   protected DBusErrorMessage(DBusMessage m, DBusException ex, Object... parameters)
   {
      super(null, null, null, "", parameters, 0, m.getSerial());
      this.type = DBusConnection.dollar_pattern.matcher(ex.getClass().getName()).replaceAll(".");
      this.name = DBusConnection.dollar_pattern.matcher(ex.getClass().getName()).replaceAll(".");
      this.destination = m.getSource();
   }
   /**
    * Create an error message.
    * @param destination The destination for the error message.
    * @param parameters The error parameters (usually a String message).
    * @param ex Exception that caused this message
    */
  protected DBusErrorMessage(String destination, DBusException ex, Object... parameters)
   {
      super(null, null, null, "", parameters, 0);
      this.type = DBusConnection.dollar_pattern.matcher(ex.getClass().getName()).replaceAll(".");
      this.name = DBusConnection.dollar_pattern.matcher(ex.getClass().getName()).replaceAll(".");
      this.destination = destination;
   }
   /**
    * Returns the destination of the error, if any.
    */
   public String getDestination() { return destination; }
   private static native Class<? extends DBusExecutionException> createExceptionClass(String name);
   /**
    * Turns this into an exception of the correct type
    */
   public DBusExecutionException getException()
   {
      try {
         Class<? extends DBusExecutionException> c = createExceptionClass(type);
         if (null == c) c = DBusExecutionException.class;
         Constructor<? extends DBusExecutionException> con = c.getConstructor(String.class);
         DBusExecutionException ex;
         if (null == parameters || 0 == parameters.length)
            ex = con.newInstance("");
         else {
            String s = "";
            for (Object o: parameters)
               s += o + " ";
            ex = con.newInstance(s.trim());
         }
         ex.setType(type);
         return ex;
      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         DBusExecutionException ex;
         if (null == parameters || 0 == parameters.length)
            ex = new DBusExecutionException("");
         else {
            String s = "";
            for (Object o: parameters)
               s += o + " ";
            ex = new DBusExecutionException(s.trim());
         }
         ex.setType(type);
         return ex;
      }
   }
   /**
    * Throw this as an exception of the correct type
    */
   public void throwException() throws DBusExecutionException
   {
      throw getException();
   }
}
