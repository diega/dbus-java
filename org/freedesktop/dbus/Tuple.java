/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Vector;

public class Tuple
{
   String sig;
   Type[] types;
   Object[] parameters;
   public Tuple(Type[] types, Object... parameters) throws DBusExecutionException
   {
      try {
         this.types = types;
         StringBuffer sb = new StringBuffer();
         for (Type t: types) {
            String[] ss = DBusConnection.getDBusType(t);
            for (String s: ss)
               sb.append(s);
         }
         sig = sb.toString();
         this.parameters = DBusConnection.convertParameters(parameters, types);
      } catch (DBusException DBe) { throw new DBusExecutionException(DBe.getMessage());
      } catch (Exception e) { 
         throw new DBusExecutionException("Tuple construction failure: "+e.getMessage()); 
      }
   }
   public Tuple(String typesig, Object... parameters) throws DBusExecutionException
   {
      try {
         this.sig = typesig;
         Vector<Type> v = new Vector<Type>();
         DBusConnection.getJavaType(typesig, v, -1);
         types = v.toArray(new Type[0]);
         this.parameters = DBusConnection.deSerializeParameters(parameters, types);
      } catch (DBusException DBe) { throw new DBusExecutionException(DBe.getMessage()); 
      } catch (Exception e) { 
         throw new DBusExecutionException("Tuple construction failure: "+e.getMessage()); 
      }
   }
   public String getString()
   {
      return sig;
   }
   public Type[] getTypes()
   {
      return types;
   }
   public Object[] getParameters()
   {
      return parameters;
   }
   public String toString()
   {
      StringBuffer s = new StringBuffer();
      s.append("Tuple<");
      for (Type t: types) {
         if (t instanceof Class)
            s.append(((Class) t).getName());
         else if (t instanceof ParameterizedType)
            s.append(((Class) ((ParameterizedType) t).getRawType()).getName());
         s.append(',');
      }
      s.append("> {");
      for (Object o: parameters) {
         s.append(o.toString());
         s.append(',');
      }
      s.append('}');
      return s.toString();
   }
}
