package org.freedesktop.dbus;

import java.lang.reflect.Type;

class TypeSignature 
{
   String sig;
   public TypeSignature(String sig)
   {
      this.sig = sig;
   }
   public TypeSignature(Type[] types) throws DBusException
   {
      StringBuffer sb = new StringBuffer();
      for (Type t: types) {
         String[] ts = DBusConnection.getDBusType(t);
         for (String s: ts)
            sb.append(s);
      }
      this.sig = sb.toString();
   }
   public String getSig()
   { 
      return sig;
   }
}
