package test;

import java.io.IOException;

public class MessageTypeException extends IOException implements NonFatalException
{
   public MessageTypeException(String message)
   {
      super(message);
   }
}
