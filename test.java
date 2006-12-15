
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
      public static final byte BIG = 'B';
      public static final byte LITTLE = 'l';
      public static final byte METHOD_CALL = 1;
      public static final byte METHOD_RETURN = 2;
      public static final byte ERROR = 3;
      public static final byte SIGNAL = 4;
      public static final byte PROTOCOL = 1;
      public static final byte PATH = 1;
      public static final byte INTERFACE = 2;
      public static final byte MEMBER = 3;
      public static final byte ERROR_NAME = 4;
      public static final byte REPLY_SERIAL = 5;
      public static final byte DESTINATION = 6;
      public static final byte SENDER = 7;
      public static final byte SIGNATURE = 8;
      private boolean big;
      private List<byte[]> wiredata;
      private Map<Byte, String> headers;
      public Message(byte endian, byte type, byte flags, byte protver)
      {
         wiredata = new Vector<byte[]>();
         headers = new HashMap<Byte, String>();
         wiredata.add(new byte[] {endian, type, flags, protver});
         big = (BIG == endian);
      }
      public Message(byte[] msg, byte[] headers, byte[] body) 
      {
         big = (msg[0] == BIG);
         wiredata = new Vector<byte[]>();
         wiredata.add(msg);
         wiredata.add(headers);
         wiredata.add(body);
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
      public void appendBytes(byte[] buf) { wiredata.add(buf); }
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

      public void appendUInt(long l) { if (big) appendUIntBig(l); else appendUIntLittle(l); }
      private void appendUIntBig(long l) 
      {
         byte[] buf = new byte[4];
         for (int i = 3; i >= 0; i--) {
            buf[i] = (byte) (l & 0xFF);
            l <<= 8;
         }
         wiredata.add(buf);
      }
      private void appendUIntLittle(long l) 
      {
         byte[] buf = new byte[4];
         for (int i = 0; i < 4; i++) {
            buf[i] = (byte) (l & 0xFF);
            l <<= 8;
         }
         wiredata.add(buf);
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

      Message m = new Message(Message.BIG, Message.METHOD_CALL, (byte) 0, Message.PROTOCOL);
      m.appendUInt(0);
      m.appendUInt(1);
      m.appendUInt(110);
      m.appendHeader(Message.PATH, "o", "/org/freedesktop/DBus");
      m.appendHeader(Message.DESTINATION, "s", "org.freedesktop.DBus");
      m.appendHeader(Message.INTERFACE, "s", "org.freedesktop.DBus");
      m.appendHeader(Message.MEMBER, "s", "Hello");
      Debug.print(m);
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
   }
}
