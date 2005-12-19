package org.freedesktop.dbus;

class MethodReply extends DBusMessage
{
   MethodCall call;
   String objectpath;
   String destination;
   protected MethodReply(String source, String objectpath, String type, String name, Object[] parameters, long serial, long replyserial)
   {
      super(source, type, name, parameters, serial, replyserial);
      this.objectpath = objectpath;
   }
   public MethodReply(MethodCall m, Object... parameters)
   {
      super(null, m.getType(), m.getName(), null, 0, m.getReplySerial());
      if (1 == parameters.length && parameters[0] instanceof Tuple) 
         this.parameters = ((Tuple) parameters[0]).getParameters();         
      else this.parameters = parameters;
      this.call = m;
      this.objectpath = m.getObjectPath();
      this.destination = m.getSource();
   }
   public MethodCall getCall() { return call; }
   protected void setCall(MethodCall call) { this.call = call; this.replyserial = call.getSerial(); this.destination = call.getSource();}
}
