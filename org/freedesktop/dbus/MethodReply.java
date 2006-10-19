/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

class MethodReply extends DBusMessage
{
   MethodCall call;
   String objectpath;
   String destination;
   protected MethodReply(String source, String objectpath, String type, String name, String sig, Object[] parameters, long serial, long replyserial)
   {
      super(source, type, name, sig, parameters, serial, replyserial);
      this.objectpath = objectpath;
   }
   public MethodReply(MethodCall m, Object... parameters)
   {
      super(null, m.getType(), m.getName(), "", null, 0, m.getReplySerial());
      if (1 == parameters.length && parameters[0] instanceof Tuple) 
         this.parameters = ((Tuple) parameters[0]).getParameters();         
      else this.parameters = parameters;
      this.call = m;
      this.objectpath = m.getObjectPath();
      this.destination = m.getSource();
      this.replyserial = m.getSerial();
   }
   public MethodCall getCall() { return call; }
   protected void setCall(MethodCall call) { this.call = call; this.replyserial = call.getSerial(); this.destination = call.getSource();}
   public String getDestination() { return destination; }
   public String getObjectPath() { return objectpath; }

}
