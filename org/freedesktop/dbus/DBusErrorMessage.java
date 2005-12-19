package org.freedesktop.dbus;

/**
 * Represents an error message sent over the Bus.
 * Any errors not associated with a method call are queued by the Bus 
 * and may be retrieved by calling DBusConnection.getError().
 * @see DBusConnection#getError()
 */
public abstract class DBusErrorMessage extends DBusMessage
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
   DBusErrorMessage(String source, String destination, String name, Object[] parameters, long serial, long replyserial)
   {
      super(source, null, name, parameters, serial, replyserial);
      this.destination = destination;
   }
   /**
    * Create an error message.
    * @param m The message this is a reply to.
    * @param parameters The error parameters (usually a String message).
    */
   protected DBusErrorMessage(DBusMessage m, Object... parameters)
   {
      super(null, null, null, parameters, 0, m.getSerial());
      this.type = getClass().getName();
      this.name = getClass().getName();
      this.destination = m.getSource();
   }
   /**
    * Create an error message.
    * @param destination The destination for the error message.
    * @param parameters The error parameters (usually a String message).
    */
  protected DBusErrorMessage(String destination, Object... parameters)
   {
      super(null, null, null, parameters, 0);
      this.type = getClass().getName();
      this.name = getClass().getName();
      this.destination = destination;
   }
   /**
    * Returns the destination of the error, if any.
    */
   public String getDestination() { return destination; }
}
