package org.freedesktop.dbus;

class InternalErrorMessage extends DBusErrorMessage
{
   InternalErrorMessage(String source, String destination, String name, Object[] parameters, long serial, long replyserial)
   {
      super(source, destination, name, parameters, serial, replyserial);
   }
   protected InternalErrorMessage(DBusMessage m, Object... parameters)
   {
      super(m, parameters);
      this.type = getClass().getName();
      this.name = getClass().getName();
   }
   protected InternalErrorMessage(String destination, Object... parameters)
   {
      super(destination, parameters);
      this.type = getClass().getName();
      this.name = getClass().getName();
   }
}
