/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;

import cx.ath.matthew.debug.Debug;

public class DBusSignal extends Message
{
   DBusSignal() { }
   public DBusSignal(String path, String iface, String member, String sig, Object... args) throws DBusException
   {
      super(Message.Endian.BIG, Message.MessageType.SIGNAL, (byte) 0);

      if (null == path || null == member || null == iface)
         throw new MessageFormatException("Must specify object path, interface and signal name to Signals.");
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
         setArgs(args);
      }

      blen = new byte[4];
      appendBytes(blen);
      append("ua(yv)", ++serial, hargs.toArray());
      pad((byte)8);

      long c = bytecounter;
      if (null != sig) append(sig, args);
      marshallint(bytecounter-c, blen, 0, 4);
      bodydone = true;
   }
   static class internalsig extends DBusSignal
   {
      public internalsig(String source, String objectpath, String type, String name, String sig, Object[] parameters, long serial) throws DBusException
      {
         super(source, objectpath, type, name, sig, parameters, serial);
      }
   }
   private static Map<Class, Type[]> typeCache = new HashMap<Class, Type[]>();
   private static Map<Class, Constructor> conCache = new HashMap<Class, Constructor>();
   private Class<? extends DBusSignal> c;
   private boolean bodydone = false;
   private byte[] blen;
   
   static DBusSignal createSignal(Class<? extends DBusSignal> c, String source, String objectpath, String sig, long serial, Object... parameters) throws DBusException
   {
      String type = "";
      if (null != c.getEnclosingClass())
         type = DBusConnection.dollar_pattern.matcher(c.getEnclosingClass().getName()).replaceAll(".");
      DBusSignal s = new internalsig(source, objectpath, type, c.getSimpleName(), sig, parameters, serial);
      s.c = c;
      return s;
   }
   @SuppressWarnings("unchecked")
   private static Class<? extends DBusSignal> createSignalClass(String name)
   {
      Class<? extends DBusSignal> c = null;
      do {
         try {
            c = (Class<? extends DBusSignal>) Class.forName(name);
         } catch (ClassNotFoundException CNFe) {}
         name = name.replaceAll("\\.([^\\.]*)$", "\\$$1");
      } while (null == c && name.matches(".*\\..*"));
      return c;
   }
   DBusSignal createReal(DBusConnection conn) throws DBusException
   {
      if (null == c) 
         c = createSignalClass(getInterface()+"$"+getName());
      if (Debug.debug) Debug.print(Debug.DEBUG, "Converting signal to type: "+c);
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
         DBusSignal s;
         Object[] args = Marshalling.deSerializeParameters(getParameters(), types, conn);
         if (null == args) s = (DBusSignal) con.newInstance(getPath());
         else {
            Object[] params = new Object[args.length + 1];
            params[0] = getPath();
            System.arraycopy(args, 0, params, 1, args.length);

            if (Debug.debug) Debug.print(Debug.DEBUG, "Creating signal of type "+c+" with parameters "+Arrays.deepToString(params));
            s = (DBusSignal) con.newInstance(params);
         }
         s.headers = headers;
         s.wiredata = wiredata;
         s.bytecounter = wiredata.length;
         return s;
      } catch (Exception e) { 
         if (DBusConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
         throw new DBusException(e.getMessage());
      }
   }
   /** 
    * Create a new signal.
    * This contructor MUST be called by all sub classes.
    * @param objectpath The path to the object this is emitted from.
    * @param args The parameters of the signal.
    * @throws DBusException This is thrown if the subclass is incorrectly defined.
    */
   protected DBusSignal(String objectpath, Object... args) throws DBusException
   {
      super(Message.Endian.BIG, Message.MessageType.SIGNAL, (byte) 0);

      if (!objectpath.matches(DBusConnection.OBJECT_REGEX)) throw new DBusException("Invalid object path ("+objectpath+")");

      Class tc = getClass();
      String member = tc.getSimpleName();
      String iface = null;
      Class enc = tc.getEnclosingClass();
      if (null == enc ||
            !DBusInterface.class.isAssignableFrom(enc) ||
            enc.getName().equals(enc.getSimpleName()))
         throw new DBusException("Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
      else
         iface = DBusConnection.dollar_pattern.matcher(enc.getName()).replaceAll(".");

      headers.put(Message.HeaderField.PATH,objectpath);
      headers.put(Message.HeaderField.MEMBER,member);
      headers.put(Message.HeaderField.INTERFACE,iface);

      Vector<Object> hargs = new Vector<Object>();
      hargs.add(new Object[] { Message.HeaderField.PATH, new Object[] { ArgumentType.OBJECT_PATH_STRING, objectpath } });
      hargs.add(new Object[] { Message.HeaderField.INTERFACE, new Object[] { ArgumentType.STRING_STRING, iface } });
      hargs.add(new Object[] { Message.HeaderField.MEMBER, new Object[] { ArgumentType.STRING_STRING, member } });

      String sig = null;
      if (0 < args.length) {
         try {
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
            sig = Marshalling.getDBusType(types);
            hargs.add(new Object[] { Message.HeaderField.SIGNATURE, new Object[] { ArgumentType.SIGNATURE_STRING, sig } });
            headers.put(Message.HeaderField.SIGNATURE,sig);
            setArgs(args);
         } catch (Exception e) {
            if (DBusConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
            throw new DBusException("Failed to add signal parameters: "+e.getMessage());
         }
      }

      blen = new byte[4];
      appendBytes(blen);
      append("ua(yv)", ++serial, hargs.toArray());
      pad((byte)8);
   }
   void appendbody(DBusConnection conn) throws DBusException
   {
      if (bodydone) return;

      Type[] types = typeCache.get(getClass());
      Object[] args = Marshalling.convertParameters(getParameters(), types, conn);
      setArgs(args);
      String sig = getSig();

      long c = bytecounter;
      if (0 < args.length) append(sig, args);
      marshallint(bytecounter-c, blen, 0, 4);
      bodydone = true;
   }
}
