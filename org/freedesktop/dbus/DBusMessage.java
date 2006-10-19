/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

/**
 * Class to represent a message on the Bus. 
 * This class should not be extended, instead extend DBusSignal.
 */
public abstract class DBusMessage
{
   String source;
   String type;
   String name;
   String sig;
   protected Object[] parameters;
   long serial;
   long replyserial;
   DBusMessage() {}
   protected DBusMessage(String source, String type, String name, String sig, Object[] parameters, long serial, long replyserial)
   {
      this.source = source;
      this.type = type;
      this.name = name;
      this.parameters = parameters;
      this.serial = serial;
      this.replyserial = replyserial;
      this.sig = sig;
   }
   protected DBusMessage(String source, String type, String name, String sig, Object[] parameters, long serial)
   {
      this(source, type, name, sig, parameters, serial, 0);
   }
   /**
    * Returns the Bus ID that sent the message
    */
   public String getSource() { return source; }
   /**
    * Returns the type of the message.
    */
   public String getType() { return type; }
   /**
    * Returns the member name or error name this message represents.
    */
   public String getName() { return name; }
   /**
    * Returns the dbus signature of the parameters.
    */
   public String getSig() { return sig; }
   /**
    * Returns the message serial ID (unique for this connection)
    * @return the message serial or 0 if it has not been sent over the Bus.
    */
   public long getSerial() { return serial; }
   /**
    * If this is a reply to a message, this returns its serial.
    * @return The reply serial, or 0 if it is not a reply.
    */
   public long getReplySerial() { return replyserial; }
   protected void setSerial(long serial) { this.serial = serial; }
   protected void setSource(String source) { this.source = source; }
   protected void setReplySerial(long serial) { this.replyserial = serial; }
   /**
    * Returns the parameters to this message as an Object array.
    */
   public Object[] getParameters() { return parameters; }
   /**
    * Represent this message as a String
    */
   public String toString()
   {
      String s = getClass().getName()+"("+source+","+type+","+name+","+serial+","+replyserial+")(";
      if (null != parameters) for (Object o: parameters)
         s += o + ",";
      s += ")";
      return s;
   }
}
