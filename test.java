
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
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
   public static class MessageReader
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
   public static class MessageWriter
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
   public static class MethodCall extends Message
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
            hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, member } });
         append(HEADER_TYPE, Message.Endian.BIG, Message.MessageType.METHOD_CALL, 0, 
               Message.PROTOCOL, 0, ++serial, hargs.toArray());
         if (null != sig) append(sig, args);
      }
   }
   public static class Signal extends Message
   {
      public Signal(String path, String iface, String member, String sig, Object... args) 
      {
         super(Message.Endian.BIG);
         Vector<Object> hargs = new Vector<Object>();
         hargs.add(new Object[] { Message.HeaderField.PATH, new Object[] { ArgumentType.OBJECT_PATH_STRING, path } });
         hargs.add(new Object[] { Message.HeaderField.INTERFACE, new Object[] { ArgumentType.STRING_STRING, iface } });
         hargs.add(new Object[] { Message.HeaderField.MEMBER, new Object[] { ArgumentType.STRING_STRING, member } });
         if (null != sig)
            hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, member } });
         append(HEADER_TYPE, Message.Endian.BIG, Message.MessageType.METHOD_CALL, 0, 
               Message.PROTOCOL, 0, ++serial, hargs.toArray());
         if (null != sig) append(sig, args);
      }
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
         public static final String BYTE_STRING="y";
         public static final String BOOLEAN_STRING="b";
         public static final String INT16_STRING="n";
         public static final String UINT16_STRING="q";
         public static final String INT32_STRING="i";
         public static final String UINT32_STRING="u";
         public static final String INT64_STRING="x";
         public static final String UINT64_STRING="t";
         public static final String DOUBLE_STRING="d";
         public static final String FLOAT_STRING="f";
         public static final String STRING_STRING="s";
         public static final String OBJECT_PATH_STRING="o";
         public static final String SIGNATURE_STRING="g";
         public static final String ARRAY_STRING="a";
         public static final String VARIANT_STRING="v";
         public static final String STRUCT_STRING="r";
         public static final String STRUCT1_STRING="(";
         public static final String STRUCT2_STRING=")";
         public static final String DICT_ENTRY_STRING="e";
         public static final String DICT_ENTRY1_STRING="{";
         public static final String DICT_ENTRY2_STRING="}";

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
      public static final String HEADER_TYPE="yyyyuua(yv)";
      private boolean big;
      private List<byte[]> wiredata;
      private long bytecounter;
      private Map<Byte, String> headers;
      protected static long serial = 0;
      private static byte[][] padding;
      static {
         padding = new byte[][] {
            null,
            new byte[1],
            new byte[2],
            new byte[3],
            new byte[4],
            new byte[5],
            new byte[6],
            new byte[7] };
      }
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
               long l = demarshallint(headers, i, 4); // get string length
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
      private void appendBytes(byte[] buf) 
      {
         if (null == buf) return;
         wiredata.add(buf); bytecounter+=buf.length; 
      }
      public long demarshallint(byte[] buf, int ofs, int width)
      { return big ? demarshallintBig(buf,ofs,width) : demarshallintLittle(buf,ofs,width); }
      public static long demarshallint(byte[] buf, int ofs, byte endian, int width)
      { return endian==Endian.BIG ? demarshallintBig(buf,ofs,width) : demarshallintLittle(buf,ofs,width); }
      public static long demarshallintBig(byte[] buf, int ofs, int width)
      {
         long l = 0;
         for (int i = 0; i < width; i++) {
            l <<=8;
            l |= (buf[ofs+i] & 0xFF);
         }
         Debug.print("demarshalling(big): "+Hexdump.toHex(buf, ofs, width)+" to "+l);
         return l;
      }
      public static long demarshallintLittle(byte[] buf, int ofs, int width)
      {
         long l = 0;
         for (int i = (width-1); i >= 0; i--) {
            l <<=8;
            l |= (buf[ofs+i] & 0xFF);
         }
         Debug.print("demarshalling(little): "+Hexdump.toHex(buf, ofs, width)+" to "+l);
         return l;
      }

      public void appendint(long l, int width)
      { 
         byte[] buf = new byte[width];
         marshallint(l, buf, 0, width);
         wiredata.add(buf);
         bytecounter += width;
      }
      public void marshallint(long l, byte[] buf, int ofs, int width)
      { if (big) marshallintBig(l, buf, 0,width); else marshallintLittle(l, buf, 0,width); }
      private void marshallintBig(long l, byte[] buf, int ofs, int width) 
      {
         for (int i = (width-1); i >= 0; i--) {
            buf[i+ofs] = (byte) (l & 0xFF);
            l <<= 8;
         }
      }
      private void marshallintLittle(long l, byte[] buf, int ofs, int width) 
      {
         for (int i = 0; i < width; i++) {
            buf[i+ofs] = (byte) (l & 0xFF);
            l <<= 8;
         }
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
         pad(sigb[i]);
         switch (sigb[i]) {
            case ArgumentType.BYTE:
               appendBytes(new byte[] { ((Number) data).byteValue() });
               break;
            case ArgumentType.BOOLEAN:
               appendint(((Boolean) data).booleanValue() ? 1 : 0, 4);
               break;
            case ArgumentType.UINT32:
               appendint(((Number) data).longValue(), 4);
               break;
            case ArgumentType.INT64:
               appendint(((Number) data).longValue(), 8);
               break;
            case ArgumentType.UINT64:
               //appendint(((Number) data).longValue(), 8);
               break;
            case ArgumentType.INT32:
               appendint(((Number) data).intValue(), 4);
               break;
            case ArgumentType.UINT16:
               appendint(((Number) data).intValue(), 2);
               break;
            case ArgumentType.INT16:
               appendint(((Number) data).shortValue(), 2);
               break;
            case ArgumentType.STRING:
            case ArgumentType.OBJECT_PATH:
               String payload = (String) data;
               appendint(payload.length(), 4);
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
               pad(sigb[++i]);
               long c = bytecounter;
               int diff = i;
               for (Object o: contents) 
                  diff = appendone(sigb, i, o);
               i = diff;
               Debug.print("start: "+c+" end: "+bytecounter+" length: "+(bytecounter-c));
               marshallint(bytecounter-c-2, len, 0, 4);
               pad(sigb[i]);
               break;
            case ArgumentType.STRUCT1:
               contents = (Object[]) data;
               int j = 0;
               for (i++; sigb[i] != ArgumentType.STRUCT2; i++)
                  i = appendone(sigb, i, contents[j++]);
               break;
            case ArgumentType.VARIANT:
               contents = (Object[]) data;
               appendone(new byte[] {ArgumentType.SIGNATURE}, 0, contents[0]);
               appendone(((String) contents[0]).getBytes(), 0, contents[1]);
               break;
         }
         return i;
      }
      private void pad(byte type)
      {
         Debug.print("padding for "+(char)type);
         int a = getAlignment(type);
         if (0 == (bytecounter%a)) return;
         appendBytes(padding[(int) (a-(bytecounter%a))]);
      }
      private int getAlignment(byte type)
      {
         switch (type) {
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
            case ArgumentType.STRUCT1:
            case ArgumentType.DICT_ENTRY1:
            case ArgumentType.STRUCT2:
            case ArgumentType.DICT_ENTRY2:
               return 8;
            case ArgumentType.BYTE:
            case ArgumentType.SIGNATURE:
            case ArgumentType.VARIANT:
            default:
               return 1;
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
   public static boolean auth(BusAddress address, OutputStream out, InputStream in) throws IOException
   {
      out.write(new byte[] { 0 });
      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      if ("unix".equals(address.getType())) {
         UnixSystem uns = new UnixSystem();
         long uid = uns.getUid();
         String Uid = Hexdump.toHex((""+uid).getBytes()).replaceAll(" ","");
         out.write(("AUTH EXTERNAL "+Uid+"\r\n").getBytes());
      } else {
         out.write(("AUTH DBUS_COOKIE_SHA1\r\n").getBytes());
      }
      String s = r.readLine();
      String[] reply=s.split(" ");
      if (!"OK".equals(reply[0])) return false;
      if (null == address.getParameter("guid") || reply[1].equals(address.getParameter("guid"))) {
         out.write("BEGIN\r\n".getBytes());
         return true;
      } 
      return false;
   }
   public static void connect(BusAddress address) throws IOException
   {
      OutputStream out = null;
      InputStream in = null;
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
      
      if (!auth(address, out, in)) {
         out.close();
         System.exit(1);
      }
      mout = new MessageWriter(out);
      min = new MessageReader(in);

   }
   private static MessageReader min;
   private static MessageWriter mout;
   public static void main(String[] args) throws Exception
   {
      
      Message test = new MethodCall(":1.0", "/", "org.foo", "Hiii", null);

      //System.exit(0);

      Debug.setHexDump(true);
      BusAddress address = new BusAddress(System.getenv("DBUS_SESSION_BUS_ADDRESS"));
      Debug.print(address);

      connect(address);

      Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", null);
      mout.writeMessage(m);
      m = min.readMessage();
      Debug.print(m);
      m = min.readMessage();
      Debug.print(m);
      m = new MethodCall("org.freedesktop.DBus", "/", null, "Hello", null);
      mout.writeMessage(m);
      m = min.readMessage();
      Debug.print(m);

      m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "RequestName", "s", "org.testname");
      mout.writeMessage(m);
      m = min.readMessage();
      Debug.print(m);
      m = new Signal("/foo", "org.foo", "Foo", null);
      mout.writeMessage(m);
      m = min.readMessage();
      Debug.print(m);
   }
}
