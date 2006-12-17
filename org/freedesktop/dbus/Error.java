package test;

import java.util.Vector;

public class Error extends Message
{
   Error() { }
   public Error(String dest, String errorName, long replyserial, String sig, Object... args) 
   {
      super(Message.Endian.BIG, Message.MessageType.ERROR, (byte) 0);

      headers.put(Message.HeaderField.REPLY_SERIAL,replyserial);
      headers.put(Message.HeaderField.ERROR_NAME,errorName);
      headers.put(Message.HeaderField.DESTINATION,dest);
      
      Vector<Object> hargs = new Vector<Object>();
      hargs.add(new Object[] { Message.HeaderField.ERROR_NAME, new Object[] { ArgumentType.STRING_STRING, errorName } });
      hargs.add(new Object[] { Message.HeaderField.DESTINATION, new Object[] { ArgumentType.STRING_STRING, dest } });
      hargs.add(new Object[] { Message.HeaderField.REPLY_SERIAL, new Object[] { ArgumentType.UINT32_STRING, replyserial } });
      if (null != sig) {
         hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
         headers.put(Message.HeaderField.SIGNATURE,sig);
         this.args = args;
      }
      
      byte[] blen = new byte[4];
      appendBytes(blen);
      append("ua(yv)", serial, hargs.toArray());
      pad((byte)8);

      long c = bytecounter;
      if (null != sig) append(sig, args);
      marshallint(bytecounter-c, blen, 0, 4);
   }
}
