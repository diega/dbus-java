package test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

public class Message
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
      return l;
   }
   public static long demarshallintLittle(byte[] buf, int ofs, int width)
   {
      long l = 0;
      for (int i = (width-1); i >= 0; i--) {
         l <<=8;
         l |= (buf[ofs+i] & 0xFF);
      }
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
            pad(ArgumentType.STRING);
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
   public void pad(byte type)
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
         case 2:
            return 2;
         case ArgumentType.BOOLEAN:
         case ArgumentType.INT32:
         case ArgumentType.UINT32:
         case ArgumentType.STRING:
         case ArgumentType.OBJECT_PATH:
         case ArgumentType.ARRAY:
         case 4:
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
         case 8:
            return 8;
         case ArgumentType.BYTE:
         case ArgumentType.SIGNATURE:
         case ArgumentType.VARIANT:
         case 1:
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
