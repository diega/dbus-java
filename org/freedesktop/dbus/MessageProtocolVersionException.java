package org.freedesktop.dbus;

import java.io.IOException;

public class MessageProtocolVersionException extends IOException implements FatalException
{
   public MessageProtocolVersionException(String message)
   {
      super(message);
   }
}
