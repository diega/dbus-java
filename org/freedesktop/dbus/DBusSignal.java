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

public class DBusSignal extends Message
{
   DBusSignal() { }
   public DBusSignal(String path, String iface, String member, String sig, Object... args) 
   {
      super(Message.Endian.BIG, Message.MessageType.SIGNAL, (byte) 0);

      headers.put(Message.HeaderField.PATH,path);
      headers.put(Message.HeaderField.MEMBER,member);
      headers.put(Message.HeaderField.INTERFACE,iface);

      Vector<Object> hargs = new Vector<Object>();
      hargs.add(new Object[] { Message.HeaderField.PATH, new Object[] { ArgumentType.OBJECT_PATH_STRING, path } });
      hargs.add(new Object[] { Message.HeaderField.INTERFACE, new Object[] { ArgumentType.STRING_STRING, iface } });
      hargs.add(new Object[] { Message.HeaderField.MEMBER, new Object[] { ArgumentType.STRING_STRING, member } });

      if (null != sig) {
         hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
         headers.put(Message.HeaderField.SIGNATURE,sig);
         this.args = args;
      }

      byte[] blen = new byte[4];
      appendBytes(blen);
      append("ua(yv)", ++serial, hargs.toArray());
      pad((byte)8);

      long c = bytecounter;
      if (null != sig) append(sig, args);
      marshallint(bytecounter-c, blen, 0, 4);
   }
   private static class internalsig extends DBusSignal
   {
      public internalsig(String source, String objectpath, String type, String name, String sig, Object[] parameters, long serial)
      {
         super(source, objectpath, type, name, sig, parameters, serial);
      }
   }
   private static Map<Class, Type[]> typeCache = new HashMap<Class, Type[]>();
   private static Map<Class, Constructor> conCache = new HashMap<Class, Constructor>();
   private Class<? extends DBusSignal> c;
   
   static DBusSignal createSignal(Class<? extends DBusSignal> c, String source, String objectpath, String sig, long serial, Object... parameters) throws DBusException
   {
      String type = "";
      if (null != c.getEnclosingClass())
         type = DBusConnection.dollar_pattern.matcher(c.getEnclosingClass().getName()).replaceAll(".");
      DBusSignal s = new internalsig(source, objectpath, type, c.getSimpleName(), sig, parameters, serial);
      s.c = c;
      return s;
   }
   DBusSignal createReal() throws DBusException
   {
      Type[] types = typeCache.get(c);
      Constructor con = conCache.get(c);
      if (null == types) {
         con = c.getDeclaredConstructors()[0];
         conCache.put(c, con);
         Type[] ts = con.getGenericParameterTypes();
         types = new Type[ts.length-1];
         for (int i = 1; i < ts.length; i++)
            if (ts[i] instanceof TypeVariable)
               for (Type b: ((TypeVariable) ts[i]).getBounds())
                  types[i-1] = b;
            else
               types[i-1] = ts[i];
         typeCache.put(c, types);
      }

      try {
         parameters = DBusConnection.deSerializeParameters(parameters, types);
         DBusSignal s;
         if (null == parameters) s = (DBusSignal) con.newInstance(objectpath);
         else {
            Object[] args = new Object[parameters.length + 1];
            args[0] = objectpath;
            System.arraycopy(parameters, 0, args, 1, parameters.length);

            s = (DBusSignal) con.newInstance(args);
         }
         s.setSerial(serial);
         s.setSource(source);
         return s;
      } catch (Exception e) { 
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException(e.getMessage());
      }
   }
   /** 
    * Create a new signal.
    * This contructor MUST be called by all sub classes.
    * @param objectpath The path to the object this is emitted from.
    * @param parameters The parameters of the signal.
    * @throws DBusException This is thrown if the subclass is incorrectly defined.
    */
   protected DBusSignal(String objectpath, Object... parameters) throws DBusException
   {
      super(null, "", null, "", parameters, 0);
      if (!objectpath.matches(DBusConnection.OBJECT_REGEX)) throw new DBusException("Invalid object path");
      Class tc = getClass();
      try {
         name = tc.getSimpleName();
         if (null != tc.getEnclosingClass())
            type = DBusConnection.dollar_pattern.matcher(tc.getEnclosingClass().getName()).replaceAll(".");
         this.objectpath = objectpath;

         // convert recursively everything
         Type[] types = typeCache.get(tc);
         if (null == types) {
            Constructor con = tc.getDeclaredConstructors()[0];
            conCache.put(tc, con);
            Type[] ts = con.getGenericParameterTypes();
            types = new Type[ts.length-1];
            for (int i = 1; i <= types.length; i++) 
               if (ts[i] instanceof TypeVariable)
                  types[i-1] = ((TypeVariable) ts[i]).getBounds()[0];
               else
                  types[i-1] = ts[i];

            typeCache.put(tc, types);
         }
         if (null != parameters)
            parameters = DBusConnection.convertParameters(parameters, types);

      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException("Failed to correctly determine DBusSignal type: "+e);
      }
   }
}
