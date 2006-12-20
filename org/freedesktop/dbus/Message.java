/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

/**
 * Superclass of all messages which are sent over the Bus.
 * This class deals with all the marshalling to/from the wire format.
 */
public class Message
{
   /** Defines constants representing the endianness of the message. */
   public static interface Endian {
      public static final byte BIG = 'B';
      public static final byte LITTLE = 'l';
   }
   /** Defines constants representing the flags which can be set on a message. */
   public static interface Flags {
      public static final byte NO_REPLY_EXPECTED = 0x01;
      public static final byte NO_AUTO_START = 0x02;
   }
   /** Defines constants for each message type. */
   public static interface MessageType {
      public static final byte METHOD_CALL = 1;
      public static final byte METHOD_RETURN = 2;
      public static final byte ERROR = 3;
      public static final byte SIGNAL = 4;
   }
   /** The current protocol major version. */
   public static final byte PROTOCOL = 1;
   /** Defines constants for each valid header field type. */
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
   /** Defines constants for each argument type.
    * There are two constants for each argument type, 
    * as a byte or as a String (the _STRING version) */
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
   /** Keep a static reference to each size of padding array to prevent allocation. */
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
   /** Steps to increment the buffer array. */
   private static final int BUFFERINCREMENT = 3;

   private boolean big;
   private byte[][] wiredata;
   protected long bytecounter;
   protected Map<Byte, Object> headers;
   protected static long globalserial = 0;
   protected long serial;
   protected byte type;
   protected byte flags;
   protected byte protover;
   protected Object[] args;
   private int preallocated = 0;
   private int paofs = 0;
   private byte[] pabuf;
   private int bufferuse = 0;

   /**
    * Create a message; only to be called by sub-classes.
    * @param endian The endianness to create the message.
    * @param type The message type.
    * @param flags Any message flags.
    */
   protected Message(byte endian, byte type, byte flags)
   {
      wiredata = new byte[BUFFERINCREMENT][];
      headers = new HashMap<Byte, Object>();
      big = (Endian.BIG == endian);
      bytecounter = 0;
      serial = ++globalserial;
      this.type = type;
      this.flags = flags;
      preallocate(12);
      append("yyyy", endian, type, flags, Message.PROTOCOL);
   }
   /**
    * Create a blank message. Only to be used when calling populate.
    */
   protected Message()
   {
      wiredata = new byte[BUFFERINCREMENT][];
      headers = new HashMap<Byte, Object>();
      bytecounter = 0;
   }
   /**
    * Create a message from wire-format data.
    * @param msg D-Bus serialized data of type yyyuu
    * @param headers D-Bus serialized data of type a(yv)
    * @param body D-Bus serialized data of the signature defined in headers.
    */
   void populate(byte[] msg, byte[] headers, byte[] body) 
   {
      big = (msg[0] == Endian.BIG);
      type = msg[1];
      flags = msg[2];
      protover = msg[3];
      wiredata[0] = msg;
      wiredata[1] = headers;
      wiredata[2] = body;
      bufferuse = 3;
      serial = (Long) extract(Message.ArgumentType.UINT32_STRING, msg, 8)[0];
      bytecounter = msg.length+headers.length+body.length;
      Debug.print(headers);
      Object[] hs = extract("a(yv)", headers, 0);
      Debug.print(Arrays.deepToString(hs));
      for (Object o: (Object[]) hs[0]) {
         this.headers.put((Byte) ((Object[])o)[0], ((Object[])((Object[])o)[1])[1]);
      }
   }
   /**
    * Create a buffer of num bytes.
    * Data is copied to this rather than added to the buffer list.
    */
   private void preallocate(int num)
   {
      preallocated = 0;
      pabuf = new byte[num];
      appendBytes(pabuf);
      preallocated = num;
      paofs = 0;
   }
   /**
    * Appends a buffer to the buffer list.
    */
   protected void appendBytes(byte[] buf) 
   {
      if (null == buf) return;
      if (preallocated > 0) {
         System.arraycopy(buf, 0, pabuf, paofs, buf.length);
         paofs += buf.length;
         preallocated -= buf.length;
      } else {
         if (bufferuse == wiredata.length) {
            Debug.print("Resizing "+bufferuse);
            byte[][] temp = new byte[wiredata.length+BUFFERINCREMENT][];
            System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
            wiredata = temp;
         }
         wiredata[bufferuse++] = buf;
         bytecounter += buf.length; 
      }
   }
   /**
    * Appends a byte to the buffer list.
    */
   protected void appendByte(byte b) 
   {
      if (preallocated > 0) {
         pabuf[paofs++] = b;
         preallocated--;
      } else {
         if (bufferuse == wiredata.length) {
            Debug.print("Resizing "+bufferuse);
            byte[][] temp = new byte[wiredata.length+BUFFERINCREMENT][];
            System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
            wiredata = temp;
         }
         wiredata[bufferuse++] = new byte[] { b };
         bytecounter++; 
      }
   }
   /**
    * Demarshalls an integer of a given width from a buffer.
    * Endianness is determined from the format of the message.
    * @param buf The buffer to demarshall from.
    * @param ofs The offset to demarshall from.
    * @param width The byte-width of the int.
    */
   public long demarshallint(byte[] buf, int ofs, int width)
   { return big ? demarshallintBig(buf,ofs,width) : demarshallintLittle(buf,ofs,width); }
   /**
    * Demarshalls an integer of a given width from a buffer.
    * @param buf The buffer to demarshall from.
    * @param ofs The offset to demarshall from.
    * @param endian The endianness to use in demarshalling.
    * @param width The byte-width of the int.
    */
   public static long demarshallint(byte[] buf, int ofs, byte endian, int width)
   { return endian==Endian.BIG ? demarshallintBig(buf,ofs,width) : demarshallintLittle(buf,ofs,width); }
   /**
    * Demarshalls an integer of a given width from a buffer using big-endian format.
    * @param buf The buffer to demarshall from.
    * @param ofs The offset to demarshall from.
    * @param width The byte-width of the int.
    */
   public static long demarshallintBig(byte[] buf, int ofs, int width)
   {
      long l = 0;
      for (int i = 0; i < width; i++) {
         l <<=8;
         l |= (buf[ofs+i] & 0xFF);
      }
      return l;
   }
   /**
    * Demarshalls an integer of a given width from a buffer using little-endian format.
    * @param buf The buffer to demarshall from.
    * @param ofs The offset to demarshall from.
    * @param width The byte-width of the int.
    */
   public static long demarshallintLittle(byte[] buf, int ofs, int width)
   {
      long l = 0;
      for (int i = (width-1); i >= 0; i--) {
         l <<=8;
         l |= (buf[ofs+i] & 0xFF);
      }
      return l;
   }
   /**
    * Marshalls an integer of a given width and appends it to the message.
    * Endianness is determined from the message.
    * @param l The integer to marshall.
    * @param width The byte-width of the int.
    */
   public void appendint(long l, int width)
   { 
      byte[] buf = new byte[width];
      marshallint(l, buf, 0, width);
      appendBytes(buf);
   }
   /**
    * Marshalls an integer of a given width into a buffer.
    * Endianness is determined from the message.
    * @param l The integer to marshall.
    * @param buf The buffer to marshall to.
    * @param ofs The offset to marshall to.
    * @param width The byte-width of the int.
    */
   public void marshallint(long l, byte[] buf, int ofs, int width)
   { if (big) marshallintBig(l, buf, 0,width); else marshallintLittle(l, buf, 0,width); }
   /**
    * Marshalls an integer of a given width into a buffer using big-endian format.
    * @param l The integer to marshall.
    * @param buf The buffer to marshall to.
    * @param ofs The offset to marshall to.
    * @param width The byte-width of the int.
    */
   private void marshallintBig(long l, byte[] buf, int ofs, int width) 
   {
      for (int i = (width-1); i >= 0; i--) {
         buf[i+ofs] = (byte) (l & 0xFF);
         l <<= 8;
      }
   }
   /**
    * Marshalls an integer of a given width into a buffer using little-endian format.
    * @param l The integer to marshall.
    * @param buf The buffer to demarshall to.
    * @param ofs The offset to demarshall to.
    * @param width The byte-width of the int.
    */
   private void marshallintLittle(long l, byte[] buf, int ofs, int width) 
   {
      for (int i = 0; i < width; i++) {
         buf[i+ofs] = (byte) (l & 0xFF);
         l <<= 8;
      }
   }
   public byte[][] getWireData()
   {
      return wiredata;
   }
   /**
    * Formats the message in a human-readable format.
    */
   public String toString()
   {
      return headers.toString();
   }
   /**
    * Returns the value of the header field of a given field.
    * @param type The field to return.
    * @return The value of the field or null if unset.
    */
   public Object getHeader(byte type) { return headers.get(type); }
   /**
    * Appends a value to the message.
    * The type of the value is read from a D-Bus signature and used to marshall 
    * the value.
    * @param sigb A buffer of the D-Bus signature.
    * @param sigofs The offset into the signature corresponding to this value.
    * @param data The value to marshall.
    * @return The offset into the signature of the end of this value's type.
    */
   private int appendone(byte[] sigb, int sigofs, Object data)
   {
      int i = sigofs;
      Debug.print(bytecounter);
      Debug.print("Appending type: "+((char)sigb[i])+" value: "+data);

      // pad to the alignment of this type.
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
            // Strings are marshalled as a UInt32 with the length,
            // followed by the String, followed by a null byte.
            String payload = (String) data;
            appendint(payload.length(), 4);
            appendBytes(payload.getBytes());
            int m = payload.length()+4;
            //pad(ArgumentType.STRING);? do we need this?
            break;
         case ArgumentType.SIGNATURE:
            // Signatures are marshalled as a byte with the length,
            // followed by the String, followed by a null byte.
            // Signatures are generally short, so preallocate the array
            // for the string, length and null byte.
            payload = (String) data;
            byte[] pbytes = payload.getBytes();
            preallocate(2+pbytes.length);
            appendByte((byte) pbytes.length);
            appendBytes(pbytes);
            appendByte((byte) 0);
            break;
         case ArgumentType.ARRAY:
            // Arrays are given as a UInt32 for the length in bytes,
            // padding to the element alignment, then elements in
            // order. The length is the length from the end of the
            // initial padding to the end of the last element.
            
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
            // Structs are aligned to 8 bytes
            // and simply contain each element marshalled in order
            contents = (Object[]) data;
            int j = 0;
            for (i++; sigb[i] != ArgumentType.STRUCT2; i++)
               i = appendone(sigb, i, contents[j++]);
            break;
         case ArgumentType.DICT_ENTRY1:
            // Dict entries are the same as structs.
            contents = (Object[]) data;
            j = 0;
            for (i++; sigb[i] != ArgumentType.DICT_ENTRY2; i++)
               i = appendone(sigb, i, contents[j++]);
            break;
         case ArgumentType.VARIANT:
            // Variants are marshalled as a signature
            // followed by the value.
            contents = (Object[]) data;
            appendone(new byte[] {ArgumentType.SIGNATURE}, 0, contents[0]);
            appendone(((String) contents[0]).getBytes(), 0, contents[1]);
            break;
      }
      return i;
   }
   /**
    * Pad the message to the proper alignment for the given type.
    */
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
   /**
    * Return the alignment for a given type.
    */
   public static int getAlignment(byte type)
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
   /**
    * Append a series of values to the message.
    * @param sig The signature(s) of the value(s).
    * @param data The value(s).
    */
   public void append(String sig, Object... data)
   {
      byte[] sigb = sig.getBytes();
      int j = 0;
      for (int i = 0; i < sigb.length; i++)
         i = appendone(sigb, i, data[j++]);
   }
   /**
    * Align a counter to the given type.
    * @param current The current counter.
    * @param type The type to align to.
    * @return The new, aligned, counter.
    */
   public int align(int current, byte type)
   {
      Debug.print("aligning to "+(char)type);
      int a = getAlignment(type);
      if (0 == (current%a)) return current;
      return current+(a-(current%a));
   }
   /**
    * Demarshall one value from a buffer.
    * @param sigb A buffer of the D-Bus signature.
    * @param buf The buffer to demarshall from.
    * @param ofs An array of two ints, the offset into the signature buffer 
    *            and the offset into the data buffer. These values will be
    *            updated to the start of the next value ofter demarshalling.
    * @return The demarshalled value.
    */
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
   /** 
    * Demarshall values from a buffer.
    * @param sig The D-Bus signature(s) of the value(s).
    * @param buf The buffer to demarshall from.
    * @param ofs The offset into the data buffer to start.
    * @return The demarshalled value(s).
    */
   public Object[] extract(String sig, byte[] buf, int ofs)
   {
      return extract(sig, buf, new int[] { 0, ofs });
   }
   /**
    * Demarshall values from a buffer.
    * @param sig The D-Bus signature(s) of the value(s).
    * @param buf The buffer to demarshall from.
    * @param ofs An array of two ints, the offset into the signature 
    *            and the offset into the data buffer. These values will be
    *            updated to the start of the next value ofter demarshalling.
    * @return The demarshalled value(s).
    */
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
   /**
    * Returns the Bus ID that sent the message.
    */
   public String getSource() { return (String) headers.get(HeaderField.SENDER); }
   /**
    * Returns the destination of the message.
    */
   public String getDestination() { return (String) headers.get(HeaderField.DESTINATION); }
   /**
    * Returns the interface of the message.
    */
   public String getInterface() { return  (String) headers.get(HeaderField.INTERFACE); }
   /**
    * Returns the object path of the message.
    */
   public String getPath() { return  (String) headers.get(HeaderField.PATH); }
   /**
    * Returns the member name or error name this message represents.
    */
   public String getName() 
   { 
      if (this instanceof Error)
         return (String) headers.get(HeaderField.ERROR_NAME); 
      else
         return (String) headers.get(HeaderField.MEMBER); 
   }
   /**
    * Returns the dbus signature of the parameters.
    */
   public String getSig() { return (String) headers.get(HeaderField.SIGNATURE); }
   /**
    * Returns the message flags.
    */
   public int getFlags() { return flags; }
   /**
    * Returns the message serial ID (unique for this connection)
    * @return the message serial.
    */
   public long getSerial() { return serial; }
   /**
    * If this is a reply to a message, this returns its serial.
    * @return The reply serial, or 0 if it is not a reply.
    */
   public long getReplySerial() 
   { 
      Long l = (Long) headers.get(HeaderField.REPLY_SERIAL); 
      if (null == l) return 0;
      return l;
   }
   /**
    * Returns the parameters to this message as an Object array.
    */
   public Object[] getParameters() { return args; }
}
