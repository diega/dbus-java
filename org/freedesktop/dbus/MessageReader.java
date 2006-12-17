package test;

import java.io.InputStream;
import java.io.IOException;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

public class MessageReader
{
   private InputStream in;
   public MessageReader(InputStream in)
   {
      this.in = in;
   }
   public Message readMessage() throws IOException
   {
      byte[] buf = new byte[12];
      in.read(buf);
      byte endian = buf[0];
      byte type = buf[1];
      byte protover = buf[3];
      if (protover > Message.PROTOCOL)
         throw new MessageProtocolVersionException("Protocol version "+protover+" is unsupported");
      int bodylen = (int) Message.demarshallint(buf, 4, endian, 4);
      byte[] tbuf = new byte[4];
      in.read(tbuf);
      int headerlen = (int) Message.demarshallint(tbuf, 0, endian, 4);
      if (0 != headerlen % 8)
         headerlen += 8-(headerlen%8);
      byte[] header=new byte[headerlen+8];
      byte[] body=new byte[bodylen];
      System.arraycopy(tbuf, 0, header, 0, 4);
      in.read(header, 8, headerlen);
      in.read(body);
      Message m;
      switch (type) {
         case Message.MessageType.METHOD_CALL:
            m = new MethodCall();
            break;
         case Message.MessageType.METHOD_RETURN:
            m = new MethodReturn();
            break;
         case Message.MessageType.SIGNAL:
            m = new Signal();
            break;
         case Message.MessageType.ERROR:
            m = new Error();
            break;
         default:
            throw new MessageTypeException("Message type "+type+" unsupported");
      }
      m.populate(buf, header, body);
      return m;
   }
}
