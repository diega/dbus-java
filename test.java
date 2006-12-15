
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixSocketAddress;
import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;
import com.sun.security.auth.module.UnixSystem;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class test
{
   public static class BusAddress
   {
      private String type;
      private Map<String,String> parameters;
      public BusAddress(String address)
      {
         String[] ss = address.split(":", 2);
         type = ss[0];
         String[] ps = ss[1].split(",");
         parameters = new HashMap<String,String>();
         for (String p: ps) {
            String[] kv = p.split("=", 2);
            parameters.put(kv[0], kv[1]);
         }
      }
      public String getType() { return type; }
      public String getParameter(String key) { return parameters.get(key); }
      public String toString() { return type+": "+parameters; }
   }
   public static class Message
   {
      public static interface Endian {
         public static final byte BIG = 'B';
         public static final byte LITTLE = 'l';
      }
      public static interface MessageType {
         public static final byte METHOD_CALL = 1;
         public static final byte METHOD_RETURN = 2;
         public static final byte ERROR = 3;
         public static final byte SIGNAL = 4;
      }
      public static final byte PROTOCOL = 1;
      public static interface HeaderField {
         public static final byte PATH = 1;
         public static final byte INTERFACE = 2;
         public static final byte MEMBER = 3;
         public static final byte ERROR_NAME = 4;
         public static final byte REPLY_SERIAL = 5;
         public static final byte DESTINATION = 6;
         public static final byte SENDER = 7;
         public static final byte SIGNATURE = 8;
      }
      public static interface ArgumentType {
         public static final byte BYTE='y';
         public static final byte BOOLEAN='b';
         public static final byte INT16='n';
         public static final byte UINT16='q';
         public static final byte INT32='i';
         public static final byte UINT32='u';
         public static final byte INT64='x';
         public static final byte UINT64='t';
         public static final byte DOUBLE='d';
         public static final byte FLOAT='f';
         public static final byte STRING='s';
         public static final byte OBJECT_PATH='o';
         public static final byte SIGNATURE='g';
         public static final byte ARRAY='a';
         public static final byte VARIANT='v';
         public static final byte STRUCT='r';
         public static final byte STRUCT1='(';
         public static final byte STRUCT2=')';
         public static final byte DICT_ENTRY='e';
         public static final byte DICT_ENTRY1='{';
         public static final byte DICT_ENTRY2='}';
      }
      private boolean big;
      private List<byte[]> wiredata;
      private long bytecounter;
      private Map<Byte, String> headers;
      public Message(byte endian)
      {
         wiredata = new Vector<byte[]>();
         headers = new HashMap<Byte, String>();
         big = (Endian.BIG == endian);
         bytecounter = 0;
      }
      public Message(byte[] msg, byte[] headers, byte[] body) 
      {
         big = (msg[0] == Endian.BIG);
         wiredata = new Vector<byte[]>();
         wiredata.add(msg);
         wiredata.add(headers);
         wiredata.add(body);
         bytecounter = msg.length+headers.length+body.length;
         this.headers = new HashMap<Byte, String>();
         Hexdump.print(headers);
         for (int i = 0; i < headers.length; i++) {
            Debug.print(i);
            byte type = headers[i++]; // get type
            Debug.print("header: "+type+" sig: "+((char) headers[i+1]));
            if (headers[i+1] == 's') {
               i+=headers[i]+2; // skip sig
               Debug.print(i);
               long l = demarshallUint(headers, i); // get string length
               i+=4;
               String value = new String(headers, i++, (int) l);
               i+=l;
               Debug.print("header: "+type+" = "+value);
               this.headers.put(type, value);
            } else if (headers[i+1] == 'u')
               i+=headers[i]+2;
            else if (headers[i+1] == 'g') {
               i+=headers[i]+2;
               byte l = headers[i++];
               String value = new String(headers, i++, (int) l);
               i += l;
               Debug.print("header: "+type+" = "+value);
               this.headers.put(type, value);
            }
            while (i%8 != 7) i++; // skip to alignment boundry
         }

      }
      public void appendBytes(byte[] buf) { wiredata.add(buf); bytecounter+=buf.length; }
      public long demarshallUint(byte[] buf, int ofs)
      { return big ? demarshallUintBig(buf,ofs) : demarshallUintLittle(buf,ofs); }
      public long demarshallUintBig(byte[] buf, int ofs)
      {
         long l = 0;
         for (int i = 0; i < 4; i++) {
            l <<=8;
            l |= buf[ofs+i];
         }
         Debug.print("demarshalling(big): "+Hexdump.toHex(buf, ofs, 4)+" to "+l);
         return l;
      }
      public long demarshallUintLittle(byte[] buf, int ofs)
      {
         long l = 0;
         for (int i = 3; i >= 0; i--) {
            l <<=8;
            l |= buf[ofs+i];
         }
         Debug.print("demarshalling(little): "+Hexdump.toHex(buf, ofs, 4)+" to "+l);
         return l;
      }

      public void appendUInt(long l)
      { 
         byte[] buf = new byte[4];
         marshallUInt(l, buf, 0);
         wiredata.add(buf);
         bytecounter += 4;
      }
      public void marshallUInt(long l, byte[] buf, int ofs)
      { if (big) marshallUIntBig(l, buf, 0); else marshallUIntLittle(l, buf, 0); }
      private void marshallUIntBig(long l, byte[] buf, int ofs) 
      {
         for (int i = 3; i >= 0; i--) {
            buf[i+ofs] = (byte) (l & 0xFF);
            l <<= 8;
         }
      }
      private void marshallUIntLittle(long l, byte[] buf, int ofs) 
      {
         for (int i = 0; i < 4; i++) {
            buf[i+ofs] = (byte) (l & 0xFF);
            l <<= 8;
         }
      }
      public void appendHeader(byte type, String sig, String payload)
      {
         appendBytes(new byte[] {type, (byte) sig.length()});
         appendBytes(sig.getBytes());
         appendBytes(new byte[1]);
         appendUInt(payload.length());
         appendBytes(payload.getBytes());
         int m = sig.length()+payload.length()+7;
         Debug.print((m/8+1)*8-m);
         appendBytes(new byte[(m/8+1)*8-m]);
         headers.put(type, payload);
      }
      public byte[][] getWireData()
      {
         return wiredata.toArray(new byte[0][]);
      }
      public String toString()
      {
         return headers.toString();
      }
      public String getHeader(byte type) { return headers.get(type); }
      private int appendone(byte[] sigb, int sigofs, Object data)
      {
         int i = sigofs;
         Debug.print(bytecounter);
         Debug.print("Appending type: "+((char)sigb[i])+" value: "+data);
         switch (sigb[i]) {
            case ArgumentType.BYTE:
               appendBytes(new byte[] { ((Number) data).byteValue() });
               break;
            case ArgumentType.BOOLEAN:
               appendUInt(((Boolean) data).booleanValue() ? 1 : 0);
               break;
            case ArgumentType.UINT32:
               appendUInt(((Number) data).longValue());
               break;
            case ArgumentType.INT64:
               //appendUInt64(((Number) data).longValue());
               break;
            case ArgumentType.INT32:
               appendUInt(((Number) data).intValue());
               break;
            case ArgumentType.UINT16:
               //appendUInt16(((Number) data).intValue());
               break;
            case ArgumentType.INT16:
               //appendUInt16(((Number) data).shortValue());
               break;
            case ArgumentType.STRING:
            case ArgumentType.OBJECT_PATH:
               String payload = (String) data;
               appendUInt(payload.length());
               appendBytes(payload.getBytes());
               int m = payload.length()+4;
               appendBytes(new byte[(m/4+1)*4-m]);
               break;
            case ArgumentType.SIGNATURE:
               payload = (String) data;
               appendBytes(new byte[] { (byte) payload.length() });
               appendBytes(payload.getBytes());
               appendBytes(new byte[1]);
               break;
            case ArgumentType.ARRAY:
               byte[] len = new byte[4];
               appendBytes(len);
               Object[] contents = (Object[]) data;
               int extra = getAlignment(sigb[i++])-4;
               if (extra > 0)
                  appendBytes(new byte[extra]);
               long c = bytecounter;
               int diff = i;
               for (Object o: contents) 
                  diff = appendone(sigb, i, o);
               i = diff;
               marshallUInt(bytecounter-c-2, len, 0);
               break;
            case ArgumentType.STRUCT1:
               contents = (Object[]) data;
               int j = 0;
               for (i++; sigb[i] != ArgumentType.STRUCT2; i++)
                  i = appendone(sigb, i, contents[j++]);
               break;
            case ArgumentType.VARIANT:
               contents = (Object[]) data;
               appendone(new byte[] {'g'}, 0, contents[0]);
               appendone(((String) contents[0]).getBytes(), 0, contents[1]);
               break;
         }
         return i;
      }
      public int getAlignment(byte type)
      {
         switch (type) {
            case ArgumentType.BYTE:
            case ArgumentType.SIGNATURE:
            case ArgumentType.VARIANT:
               return 1;
            case ArgumentType.INT16:
            case ArgumentType.UINT16:
               return 2;
            case ArgumentType.BOOLEAN:
            case ArgumentType.INT32:
            case ArgumentType.UINT32:
            case ArgumentType.STRING:
            case ArgumentType.OBJECT_PATH:
            case ArgumentType.ARRAY:
               return 4;
            case ArgumentType.INT64:
            case ArgumentType.UINT64:
            case ArgumentType.DOUBLE:
            case ArgumentType.STRUCT:
            case ArgumentType.DICT_ENTRY:
               return 8;
            default:
               return 0;
         }
      }
      public void append(String sig, Object... data)
      {
         byte[] sigb = sig.getBytes();
         int j = 0;
         for (int i = 0; i < sigb.length; i++)
            i = appendone(sigb, i, data[j++]);
      }
   }
   public static void main(String[] args) throws Exception
   {
      Debug.setHexDump(true);
      BusAddress address = new BusAddress(System.getenv("DBUS_SESSION_BUS_ADDRESS"));
      Debug.print(address);
      OutputStream out = null;
      InputStream in = null;
      String guid = null;
      if ("unix".equals(address.getType())) {
         UnixSocket us = new UnixSocket();
         if (null != address.getParameter("abstract"))
            us.connect(new UnixSocketAddress(address.getParameter("abstract"), true));
         else if (null != address.getParameter("path"))
            us.connect(new UnixSocketAddress(address.getParameter("path"), false));
         us.setPassCred(true);
         in = us.getInputStream();
         out = us.getOutputStream();
      } else if ("tcp".equals(address.getType())) {
         Socket s = new Socket();
         s.connect(new InetSocketAddress(address.getParameter("host"), Integer.parseInt(address.getParameter("port"))));
         in = s.getInputStream();
         out = s.getOutputStream();
      } else {
         System.err.println("unknown address type");
         System.exit(1);
      }
      out.write(new byte[] { 0 });
      if ("unix".equals(address.getType())) {
         UnixSystem uns = new UnixSystem();
         long uid = uns.getUid();
         String Uid = Hexdump.toHex((""+uid).getBytes()).replaceAll(" ","");
         out.write(("AUTH EXTERNAL "+Uid+"\r\n").getBytes());
      } else {
         out.write(("AUTH DBUS_COOKIE_SHA1\r\n").getBytes());
      }
      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      String s = r.readLine();
      System.out.println(s);
      String[] reply=s.split(" ");
      System.out.println(reply[0]);
      System.out.println(reply[1]);
      System.out.println(reply[1].equals(address.getParameter("guid")));
      out.write("BEGIN\r\n".getBytes());
      // org.freedesktop.DBus

      Message m = new Message(Message.Endian.BIG);
      m.append("yyyyuua(yv)", Message.Endian.BIG, Message.MessageType.METHOD_CALL, 0, 
            Message.PROTOCOL, 0, 1, new Object[] {
                  new Object[] { Message.HeaderField.PATH, new Object[] { "o", "/org/freedesktop/DBus" } },
                  new Object[] { Message.HeaderField.DESTINATION, new Object[] { "s", "org.freedesktop.DBus" } },
                  new Object[] { Message.HeaderField.INTERFACE, new Object[] { "s", "org.freedesktop.DBus" } },
                  new Object[] { Message.HeaderField.MEMBER, new Object[] { "s", "Hello" } }
               });
      for (byte[] buf: m.getWireData()) {
         Hexdump.print(buf);
         out.write(buf);
      }
      out.flush();
      byte[] buf = new byte[16];
      in.read(buf);
      Hexdump.print(buf);
      int bodylen = buf[4];
      int headerlen = buf[12];
      headerlen=((headerlen/8)+1)*8;
      byte[] header=new byte[headerlen];
      in.read(header);
      Hexdump.print(header);
      byte[] body=new byte[bodylen];
      in.read(body);
      Hexdump.print(body);
      m = new Message(buf, header, body);
      Debug.print(m);
      // TODO nood to read off the signal here

      m = new Message(Message.Endian.BIG);
      m.append("yyyyuua(yv)", Message.Endian.BIG, Message.MessageType.METHOD_CALL, 0, 
            Message.PROTOCOL, 0, 2, new Object[] {
                  new Object[] { Message.HeaderField.PATH, new Object[] { "o", "/org/freedesktop/DBus" } },
                  new Object[] { Message.HeaderField.DESTINATION, new Object[] { "s", "org.freedesktop.DBus" } },
                  new Object[] { Message.HeaderField.INTERFACE, new Object[] { "s", "org.freedesktop.DBus" } },
                  new Object[] { Message.HeaderField.MEMBER, new Object[] { "s", "ListNames" } }
               }); // TODO: something here isn't being aligned right? the daemon is expecting 4 more bytes. Or the lengths in the header are wrong.
      for (byte[] buf2: m.getWireData()) {
         Hexdump.print(buf2);
         out.write(buf2);
      }
      out.flush();
      buf = new byte[16];
      in.read(buf);
      Hexdump.print(buf);
      bodylen = buf[4];
      headerlen = buf[12];
      headerlen=((headerlen/8)+1)*8;
      header=new byte[headerlen];
      in.read(header);
      Hexdump.print(header);
      body=new byte[bodylen];
      in.read(body);
      Hexdump.print(body);
      m = new Message(buf, header, body);
      Debug.print(m);

   }
}
