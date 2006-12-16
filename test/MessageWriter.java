package test;

import java.io.OutputStream;
import java.io.IOException;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

public class MessageWriter
{
   private OutputStream out;
   public MessageWriter(OutputStream out)
   {
      this.out = out;
   }
   public void writeMessage(Message m) throws IOException
   {
      for (byte[] buf: m.getWireData()) {
         Hexdump.print(buf);
         out.write(buf);
      }
      out.flush();
   }
}
