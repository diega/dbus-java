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
      byte[] buf = new byte[16];
      in.read(buf);
      Hexdump.print(buf);
      int bodylen = (int) Message.demarshallint(buf, 4, buf[0], 4);
      int headerlen = (int) Message.demarshallint(buf, 12, buf[0], 4);
      Debug.print("bodylen: "+bodylen+" headerlen: "+headerlen);
      headerlen=((headerlen/8)+1)*8;
      byte[] header=new byte[headerlen];
      in.read(header);
      Hexdump.print(header);
      byte[] body=new byte[bodylen];
      in.read(body);
      Hexdump.print(body);
      return new Message(buf, header, body);
   }
}
