/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;
class ExportedObject
{
   private String getAnnotations(AnnotatedElement c)
   {
      String ans = "";
      for (Annotation a: c.getDeclaredAnnotations()) {
         Class t = a.annotationType();
         String value = "";
         try {
            Method m = t.getMethod("value");
            value = m.invoke(a).toString();
         } catch (NoSuchMethodException NSMe) {
         } catch (InvocationTargetException ITe) {
         } catch (IllegalAccessException IAe) {}

         ans += "  <annotation name=\""+DBusConnection.dollar_pattern.matcher(t.getName()).replaceAll(".")+"\" value=\""+value+"\" />\n";
      }
      return ans;
   }
   private Map<MethodTuple,Method> getExportedMethods(Class c) throws DBusException
   {
      if (DBusInterface.class.equals(c)) return new HashMap<MethodTuple,Method>();
      Map<MethodTuple,Method> m = new HashMap<MethodTuple,Method>();
      for (Class i: c.getInterfaces())
         if (DBusInterface.class.equals(i)) {
            // don't let people export things which don't have a
            // valid D-Bus interface name
            if (i.getName().equals(i.getSimpleName()))
               throw new DBusException("DBusInterfaces cannot be declared outside a package");
            // add this class's public methods
            if (c.getName().length() > DBusConnection.MAX_NAME_LENGTH) 
               throw new DBusException("Introspected interface name exceeds 255 characters. Cannot export objects of type "+c.getName()+".");
            introspectiondata += " <interface name=\""+DBusConnection.dollar_pattern.matcher(c.getName()).replaceAll(".")+"\">\n";
            introspectiondata += getAnnotations(c);
            for (Method meth: c.getDeclaredMethods()) 
               if (Modifier.isPublic(meth.getModifiers())) {
                  String ms = "";
                  if (meth.getName().length() > DBusConnection.MAX_NAME_LENGTH) 
                     throw new DBusException("Introspected method name exceeds 255 characters. Cannot export objects with method "+meth.getName()+".");
                  introspectiondata += "  <method name=\""+meth.getName()+"\" >\n";
                  introspectiondata += getAnnotations(meth);
                  for (Class ex: meth.getExceptionTypes())
                     if (DBusExecutionException.class.isAssignableFrom(ex))
                        introspectiondata +=
                           "   <annotation name=\"org.freedesktop.DBus.Method.Error\" value=\""+DBusConnection.dollar_pattern.matcher(ex.getName()).replaceAll(".")+"\" />\n";
                  for (Type pt: meth.getGenericParameterTypes())
                     for (String s: DBusConnection.getDBusType(pt)) {
                        introspectiondata += "   <arg type=\""+s+"\" direction=\"in\"/>\n";
                        ms += s;
                     }
                  if (!Void.TYPE.equals(meth.getGenericReturnType())) {
                     if (Tuple.class.isAssignableFrom((Class) meth.getReturnType())) {
                        ParameterizedType tc = (ParameterizedType) meth.getGenericReturnType();
                        Type[] ts = tc.getActualTypeArguments();

                        for (Type t: ts)
                           if (t != null)
                              for (String s: DBusConnection.getDBusType(t))
                                 introspectiondata += "   <arg type=\""+s+"\" direction=\"out\"/>\n";
                     } else if (Object[].class.equals(meth.getGenericReturnType())) {
                        throw new DBusException("Return type of Object[] cannot be introspected properly");
                     } else
                        for (String s: DBusConnection.getDBusType(meth.getGenericReturnType()))
                        introspectiondata += "   <arg type=\""+s+"\" direction=\"out\"/>\n";
                  }
                  introspectiondata += "  </method>\n";
                  m.put(new MethodTuple(meth.getName(), ms), meth);
               }
            for (Class sig: c.getDeclaredClasses()) 
               if (DBusSignal.class.isAssignableFrom(sig)) {
                  if (sig.getSimpleName().length() > DBusConnection.MAX_NAME_LENGTH) 
                     throw new DBusException("Introspected signal name exceeds 255 characters. Cannot export objects with signals of type "+sig.getSimpleName()+".");
                  introspectiondata += "  <signal name=\""+sig.getSimpleName()+"\">\n";
                  Constructor con = sig.getConstructors()[0];
                  Type[] ts = con.getGenericParameterTypes();
                  for (int j = 1; j < ts.length; j++)
                     for (String s: DBusConnection.getDBusType(ts[j]))
                        introspectiondata += "   <arg type=\""+s+"\" direction=\"out\" />\n";
                  introspectiondata += getAnnotations(sig);
                  introspectiondata += "  </signal>\n";

               }
            introspectiondata += " </interface>\n";
         } else {
            // recurse
            m.putAll(getExportedMethods(i));
         }
      return m;
   }
   Map<MethodTuple,Method> methods;
   DBusInterface object;
   String introspectiondata;
   public ExportedObject(DBusInterface object) throws DBusException
   {
      this.object = object;
      introspectiondata = "";
      methods = getExportedMethods(object.getClass());
      introspectiondata += 
         " <interface name=\"org.freedesktop.DBus.Introspectable\">\n"+
         "  <method name=\"Introspect\">\n"+
         "   <arg type=\"s\" direction=\"out\"/>\n"+
         "  </method>\n"+
         " </interface>\n";
   }
}


