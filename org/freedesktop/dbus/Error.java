/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.util.Vector;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Error messages which can be sent over the bus.
 */
public class Error extends Message
{
   Error() { }
   public Error(String dest, String errorName, long replyserial, String sig, Object... args) 
   {
      super(Message.Endian.BIG, Message.MessageType.ERROR, (byte) 0);

      headers.put(Message.HeaderField.REPLY_SERIAL,replyserial);
      headers.put(Message.HeaderField.ERROR_NAME,errorName);
      headers.put(Message.HeaderField.DESTINATION,dest);
      
      Vector<Object> hargs = new Vector<Object>();
      hargs.add(new Object[] { Message.HeaderField.ERROR_NAME, new Object[] { ArgumentType.STRING_STRING, errorName } });
      hargs.add(new Object[] { Message.HeaderField.DESTINATION, new Object[] { ArgumentType.STRING_STRING, dest } });
      hargs.add(new Object[] { Message.HeaderField.REPLY_SERIAL, new Object[] { ArgumentType.UINT32_STRING, replyserial } });
      if (null != sig) {
         hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
         headers.put(Message.HeaderField.SIGNATURE,sig);
         this.args = args;
      }
      
      byte[] blen = new byte[4];
      appendBytes(blen);
      append("ua(yv)", serial, hargs.toArray());
      pad((byte)8);

      long c = bytecounter;
      if (null != sig) append(sig, args);
      marshallint(bytecounter-c, blen, 0, 4);
   }
   /* TODO NotNative! */
   private static native Class<? extends DBusExecutionException> createExceptionClass(String name);
   /**
    * Turns this into an exception of the correct type
    */
   public DBusExecutionException getException()
   {
      try {
         Class<? extends DBusExecutionException> c = createExceptionClass(type);
         if (null == c) c = DBusExecutionException.class;
         Constructor<? extends DBusExecutionException> con = c.getConstructor(String.class);
         DBusExecutionException ex;
         if (null == parameters || 0 == parameters.length)
            ex = con.newInstance("");
         else {
            String s = "";
            for (Object o: args)
               s += o + " ";
            ex = con.newInstance(s.trim());
         }
         ex.setType(type);
         return ex;
      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         DBusExecutionException ex;
         if (null == parameters || 0 == parameters.length)
            ex = new DBusExecutionException("");
         else {
            String s = "";
            for (Object o: args)
               s += o + " ";
            ex = new DBusExecutionException(s.trim());
         }
         ex.setType(type);
         return ex;
      }
   }
   /**
    * Throw this as an exception of the correct type
    */
   public void throwException() throws DBusExecutionException
   {
      throw getException();
   }
}
