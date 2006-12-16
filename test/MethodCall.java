package test;

import java.util.Vector;

public class MethodCall extends Message
{
   public MethodCall(String dest, String path, String iface, String member, String sig, Object... args) 
   {
      super(Message.Endian.BIG);
      Vector<Object> hargs = new Vector<Object>();
      hargs.add(new Object[] { Message.HeaderField.DESTINATION, new Object[] { ArgumentType.STRING_STRING, dest } });
      hargs.add(new Object[] { Message.HeaderField.PATH, new Object[] { ArgumentType.OBJECT_PATH_STRING, path } });
      if (null != iface)
         hargs.add(new Object[] { Message.HeaderField.INTERFACE, new Object[] { ArgumentType.STRING_STRING, iface } });
      hargs.add(new Object[] { Message.HeaderField.MEMBER, new Object[] { ArgumentType. STRING_STRING, member } });
      if (null != sig)
         hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
      append(HEADER_TYPE, Message.Endian.BIG, Message.MessageType.METHOD_CALL, 0, 
            Message.PROTOCOL, 0, ++serial, hargs.toArray());
      pad((byte)8);
      if (null != sig) append(sig, args);
   }
}
