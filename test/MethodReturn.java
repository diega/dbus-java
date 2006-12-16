package test;

import java.util.Vector;

public class MethodReturn extends Message
{
   public MethodReturn(String dest, long replyserial, String sig, Object... args) 
   {
      super(Message.Endian.BIG);
      Vector<Object> hargs = new Vector<Object>();
      hargs.add(new Object[] { Message.HeaderField.DESTINATION, new Object[] { ArgumentType.STRING_STRING, dest } });
      hargs.add(new Object[] { Message.HeaderField.REPLY_SERIAL, new Object[] { ArgumentType.UINT32_STRING, replyserial } });
      if (null != sig)
         hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
      append(HEADER_TYPE, Message.Endian.BIG, Message.MessageType.METHOD_CALL, 0, 
            Message.PROTOCOL, 0, ++serial, hargs.toArray());
      pad((byte)8);
      if (null != sig) append(sig, args);
   }
}
