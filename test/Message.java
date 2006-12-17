package test;

import java.util.Arrays;
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
   private boolean big;
   private List<byte[]> wiredata;
   protected long bytecounter;
   protected Map<Byte, Object> headers;
   protected static long globalserial = 0;
   private static byte[][] padding;
   protected long serial;
   protected byte type;
   protected byte flags;
   protected byte protover;
   protected Object[] args;
   private int preallocated = 0;
   private int paofs = 0;
   private byte[] pabuf;
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
   protected Message(byte endian, byte type, byte flags)
   {
      wiredata = new Vector<byte[]>();
      headers = new HashMap<Byte, Object>();
      big = (Endian.BIG == endian);
      bytecounter = 0;
      serial = ++globalserial;
      this.type = type;
      this.flags = flags;
      preallocate(12);
      append("yyyy", endian, type, flags, Message.PROTOCOL);
   }
   protected Message()
   {
      wiredata = new Vector<byte[]>();
      headers = new HashMap<Byte, Object>();
      bytecounter = 0;
   }
   void populate(byte[] msg, byte[] headers, byte[] body) 
   {
      big = (msg[0] == Endian.BIG);
      type = msg[1];
      flags = msg[2];
      protover = msg[3];
      wiredata.add(msg);
      wiredata.add(headers);
      wiredata.add(body);
      serial = (Long) extract(Message.ArgumentType.UINT32_STRING, msg, 8)[0];
      bytecounter = msg.length+headers.length+body.length;
      Debug.print(headers);
      Object[] hs = extract("a(yv)", headers, 0);
      Debug.print(Arrays.deepToString(hs));
      for (Object o: (Object[]) hs[0]) {
         this.headers.put((Byte) ((Object[])o)[0], ((Object[])((Object[])o)[1])[1]);
      }
   }
   private void preallocate(int num)
   {
      preallocated = 0;
      pabuf = new byte[num];
      appendBytes(pabuf);
      preallocated = num;
      paofs = 0;
   }
   protected void appendBytes(byte[] buf) 
   {
      if (null == buf) return;
      if (preallocated > 0) {
         System.arraycopy(buf, 0, pabuf, paofs, buf.length);
         paofs += buf.length;
         preallocated -= buf.length;
      } else {
         wiredata.add(buf);
         bytecounter += buf.length; 
      }
   }
   protected void appendByte(byte b) 
   {
      if (preallocated > 0) {
         pabuf[paofs++] = b;
         preallocated--;
      } else {
         wiredata.add(new byte[] { b });
         bytecounter++; 
      }
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
      appendBytes(buf);
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
   public Object getHeader(byte type) { return headers.get(type); }
   private int appendone(byte[] sigb, int sigofs, Object data)
   {
      int i = sigofs;
      Debug.print(bytecounter);
      Debug.print("Appending type: "+((char)sigb[i])+" value: "+data);
      pad(sigb[i]);
      switch (sigb[i]) {
         case ArgumentType.BYTE:
            appendByte(((Number) data).byteValue());
            break;
         case ArgumentType.BOOLEAN:
            appendint(((Boolean) data).booleanValue() ? 1 : 0, 4);
            break;
         case ArgumentType.DOUBLE:
            long l = Double.doubleToLongBits((Double) data);
            appendint(l, 8);
            break;
         case ArgumentType.FLOAT:
            int rf = Float.floatToIntBits((Float) data);
            appendint(rf, 4);
            break;
         case ArgumentType.UINT32:
            appendint(((Number) data).longValue(), 4);
            break;
         case ArgumentType.INT64:
            appendint(((Number) data).longValue(), 8);
            break;
         case ArgumentType.UINT64:
            //TODO appendint(((Number) data).longValue(), 8);
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
            byte[] pbytes = payload.getBytes();
            preallocate(2+pbytes.length);
            appendByte((byte) pbytes.length);
            appendBytes(pbytes);
            appendByte((byte) 0);
            break;
         case ArgumentType.ARRAY:
            // TODO: optimise primatives
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
         case ArgumentType.DICT_ENTRY1:
            contents = (Object[]) data;
            j = 0;
            for (i++; sigb[i] != ArgumentType.DICT_ENTRY2; i++)
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
      Debug.print(preallocated+" "+paofs+" "+bytecounter+" "+a);
      int b = (int) ((bytecounter-preallocated)%a);
      if (0 == b) return;
      a = (a-b);
      if (preallocated > 0) {
         paofs += a;
         preallocated -= a;
      } else
         appendBytes(padding[a]);
      Debug.print(preallocated+" "+paofs+" "+bytecounter+" "+a);
   }
   private int getAlignment(byte type)
   {
      switch (type) {
         case 2:
         case ArgumentType.INT16:
         case ArgumentType.UINT16:
            return 2;
         case 4:
         case ArgumentType.BOOLEAN:
         case ArgumentType.INT32:
         case ArgumentType.UINT32:
         case ArgumentType.STRING:
         case ArgumentType.OBJECT_PATH:
         case ArgumentType.ARRAY:
            return 4;
         case 8:
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
         case 1:
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
   public int align(int current, byte type)
   {
      Debug.print("aligning to "+(char)type);
      int a = getAlignment(type);
      if (0 == (current%a)) return current;
      return current+(a-(current%a));
   }
   private Object extractone(byte[] sigb, byte[] buf, int[] ofs)
   {
      Debug.print("Extracting type: "+((char)sigb[ofs[0]])+" from offset "+ofs[1]);
      Object rv = null;
      ofs[1] = align(ofs[1], sigb[ofs[0]]);
      switch (sigb[ofs[0]]) {
         case ArgumentType.BYTE:
            rv = buf[ofs[1]++];
            break;
         case ArgumentType.UINT32:
            rv = demarshallint(buf, ofs[1], 4);
            ofs[1] += 4;
            break;
         case ArgumentType.INT32:
            rv = (int) demarshallint(buf, ofs[1], 4);
            ofs[1] += 4;
            break;
         case ArgumentType.INT16:
            rv = (short) demarshallint(buf, ofs[1], 2);
            ofs[1] += 2;
            break;
         case ArgumentType.UINT16:
            rv = (int) demarshallint(buf, ofs[1], 2);
            ofs[1] += 2;
            break;
         case ArgumentType.INT64:
            rv = demarshallint(buf, ofs[1], 8);
            ofs[1] += 8;
            break;
         case ArgumentType.UINT64:
            /*TODO rv = demarshallint(buf, ofs[1], 8);*/
            ofs[1] += 8;
            break;
         case ArgumentType.DOUBLE:
            long l = demarshallint(buf, ofs[1], 8);
            ofs[1] += 8;
            rv = Double.longBitsToDouble(l);
            break;
         case ArgumentType.FLOAT:
            int rf = (int) demarshallint(buf, ofs[1], 4);
            ofs[1] += 4;
            rv = Float.intBitsToFloat(rf);
            break;
         case ArgumentType.BOOLEAN:
            rf = (int) demarshallint(buf, ofs[1], 4);
            ofs[1] += 4;
            rv = (1==rf)?Boolean.TRUE:Boolean.FALSE;
            break;
         case ArgumentType.ARRAY:
            // TODO: optimise primatives
            long length = demarshallint(buf, ofs[1], 4);
            ofs[1] += 4;
            ofs[1] = align(ofs[1], sigb[++ofs[0]]);
            int ofssave = ofs[0];
            long end = ofs[1]+length;
            Vector<Object> contents = new Vector<Object>();
            while (ofs[1] < end) {
               ofs[0] = ofssave;
               contents.add(extractone(sigb, buf, ofs));
            }
            rv = contents.toArray();
            break;
         case ArgumentType.STRUCT1:
            contents = new Vector<Object>();
            while (sigb[++ofs[0]] != ArgumentType.STRUCT2) {
               contents.add(extractone(sigb, buf, ofs));
            }
            ofs[0]++;
            rv = contents.toArray();
            break;
         case ArgumentType.DICT_ENTRY1:
            contents = new Vector<Object>();
            while (sigb[++ofs[0]] != ArgumentType.DICT_ENTRY2) {
               contents.add(extractone(sigb, buf, ofs));
            }
            ofs[0]++;
            rv = contents.toArray();
            break;
         case ArgumentType.VARIANT:
            int[] newofs = new int[] { 0, ofs[1] };
            String sig = (String) extract(ArgumentType.SIGNATURE_STRING, buf, newofs)[0];
            newofs[0] = 0;
            rv = new Object[] { sig, extract(sig, buf, newofs)[0] };
            ofs[1] = newofs[1];
            break;
         case ArgumentType.STRING:
         case ArgumentType.OBJECT_PATH:
            length = demarshallint(buf, ofs[1], 4);
            ofs[1] += 4;
            rv = new String(buf, ofs[1], (int)length);
            ofs[1] += length + 1;
            break;
         case ArgumentType.SIGNATURE:
            length = (buf[ofs[1]++] & 0xFF);
            rv = new String(buf, ofs[1], (int)length);
            ofs[1] += length + 1;
            break;
      }
      Debug.print("Extracted: "+rv);
      return rv;
   }
   public Object[] extract(String sig, byte[] buf, int ofs)
   {
      return extract(sig, buf, new int[] { 0, ofs });
   }
   public Object[] extract(String sig, byte[] buf, int[] ofs)
   {
      Debug.print("extract("+sig+",#"+buf.length+", {"+ofs[0]+","+ofs[1]+"}");
      Vector<Object> rv = new Vector<Object>();
      byte[] sigb = sig.getBytes();
      for (int[] i = ofs; i[0] < sigb.length; i[0]++) {
         rv.add(extractone(sigb, buf, i));
      }
      return rv.toArray();
   }
}
