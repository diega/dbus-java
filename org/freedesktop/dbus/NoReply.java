package org.freedesktop.dbus;

/**
 * Thrown by a DBusAsyncReply if no reply has been received 
 * and you are trying to recover the reply.
 */
public class NoReply extends DBusExecutionException
{
   public NoReply(String message)
   {
      super (message);
   }
}
