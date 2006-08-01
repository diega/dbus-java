package org.freedesktop.dbus;

class DBusMatchRule
{
   /* signal, error, method_call, method_reply */
   private String type;
   private String iface;
   private String member;
   private String object;
   private String source;
   public DBusMatchRule(String type, String iface, String member)
   {
      this.type = type;
      this.iface = iface;
      this.member = member;
   }
   public DBusMatchRule(DBusExecutionException e)
   {
      iface = DBusConnection.dollar_pattern.matcher(e.getClass().getName()).replaceAll(".");
      member = null;
      type = "error";
   }
   public DBusMatchRule(DBusMessage m)
   {
      iface = m.getType();
      member = m.getName();
      if (m instanceof DBusSignal)
         type = "signal";
      else if (m instanceof DBusErrorMessage) {
         type = "error";
         member = null;
      }
      else if (m instanceof MethodCall)
         type = "method_call";
      else if (m instanceof MethodReply)
         type = "method_reply";
   }
   public DBusMatchRule(Class<? extends DBusInterface> c, String method)
   {
      iface = DBusConnection.dollar_pattern.matcher(c.getName()).replaceAll(".");
      member = method;
      type = "method_call";
   }
   public DBusMatchRule(Class c, String source, String object) throws DBusException
   {
      this(c);
      this.source = source;
      this.object = object;
   }
   public DBusMatchRule(Class c) throws DBusException
   {
      if (DBusInterface.class.isAssignableFrom(c)) {
         iface = DBusConnection.dollar_pattern.matcher(c.getName()).replaceAll(".");
         member = null;
         type = null;
      }
      else if (DBusSignal.class.isAssignableFrom(c)) {
         iface = DBusConnection.dollar_pattern.matcher(c.getEnclosingClass().getName()).replaceAll(".");
         member = c.getSimpleName();
         type = "signal";
      }
      else if (DBusErrorMessage.class.isAssignableFrom(c)) {
         iface = DBusConnection.dollar_pattern.matcher(c.getName()).replaceAll(".");
         member = null;
         type = "error";
      }
      else if (DBusExecutionException.class.isAssignableFrom(c)) {
         iface = DBusConnection.dollar_pattern.matcher(c.getClass().getName()).replaceAll(".");
         member = null;
         type = "error";
      }
      else
         throw new DBusException("Invalid type for match rule ("+c+")");
   }
   public String toString()
   {
      String s = null;
      if (null != type) s = null == s ? "type='"+type+"'" : s + ",type='"+type+"'";
      if (null != member) s = null == s ? "member='"+member+"'" : s + ",member='"+member+"'";
      if (null != iface) s = null == s ? "interface='"+iface+"'" : s + ",interface='"+iface+"'";
      if (null != source) s = null == s ? "sender='"+source+"'" : s + ",sender='"+source+"'";
      if (null != object) s = null == s ? "path='"+object+"'" : s + ",path='"+object+"'";
      return s;
   }
   public String getType() { return type; }
   public String getInterface() { return iface; }
   public String getMember() { return member; }
   public String getSource() { return source; }
   public String getObject() { return object; }
   
}
