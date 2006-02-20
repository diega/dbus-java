package org.freedesktop.dbus;

import java.lang.annotation.Annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.freedesktop.DBus;

class SignalTuple
{
   String type;
   String name;
   public SignalTuple(String type, String name)
   {
      this.type = type;
      this.name = name;
   }
   public boolean equals(Object o)
   {
       return o.getClass().equals(SignalTuple.class)
            && ((SignalTuple) o).type.equals(this.type)
            && ((SignalTuple) o).name.equals(this.name);
   }
   public int hashCode()
   {
      return type.hashCode()+name.hashCode();
   }
}

class MethodTuple
{
   String type;
   String name;
   public MethodTuple(String type, String name)
   {
      this.type = type;
      this.name = name;
   }
   public boolean equals(Object o)
   {
      return o.getClass().equals(MethodTuple.class)
            && ((MethodTuple) o).type.equals(this.type)
            && ((MethodTuple) o).name.equals(this.name);
   }
   public int hashCode()
   {
      return type.hashCode()+name.hashCode();
   }
}
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

         ans += "  <annotation name=\""+t.getName().replaceAll("[$]",".")+"\" value=\""+value+"\" />\n";
      }
      return ans;
   }
   private Map<MethodTuple,Method> getExportedMethods(Class c) throws DBusException
   {
      if (DBusInterface.class.equals(c)) return new HashMap<MethodTuple,Method>();
      Map<MethodTuple,Method> m = new HashMap<MethodTuple,Method>();
      for (Class i: c.getInterfaces())
         if (DBusInterface.class.equals(i)) {
            // add this class's public methods
            introspectiondata += " <interface name=\""+c.getName()+"\">\n";
            introspectiondata += getAnnotations(c);
            for (Method meth: c.getDeclaredMethods()) 
               if (Modifier.isPublic(meth.getModifiers())) {
                  m.put(new MethodTuple(c.getName().replaceAll("[$]","."), meth.getName()), meth);
                  introspectiondata += "  <method name=\""+meth.getName()+"\" >\n";
                  introspectiondata += getAnnotations(meth);
                  for (Class ex: meth.getExceptionTypes())
                     if (DBusExecutionException.class.isAssignableFrom(ex))
                        introspectiondata +=
                           "   <annotation type=\"org.freedesktop.DBus.Method.Error\" value=\""+ex.getName().replaceAll("[$]",".")+"\" />\n";
                  for (Type pt: meth.getGenericParameterTypes())
                     introspectiondata +=
                        "   <arg type=\""+DBusConnection.getDBusType(pt)+"\" direction=\"in\"/>\n";
                  if (!Void.TYPE.equals(meth.getGenericReturnType())) {
                     if (Tuple.class.isAssignableFrom((Class) meth.getReturnType())) {
                        for (Type t: ((ParameterizedType) meth.getGenericReturnType()).getActualTypeArguments())
                           introspectiondata +=
                              "   <arg type=\""+DBusConnection.getDBusType(t)+"\" direction=\"out\"/>\n";
                     } else if (Object[].class.equals(meth.getGenericReturnType())) {
                        throw new DBusException("Return type of Object[] cannot be introspected properly");
                     } else
                        introspectiondata +=
                        "   <arg type=\""+DBusConnection.getDBusType(meth.getGenericReturnType())+"\" direction=\"out\"/>\n";
                  }
                  introspectiondata += "  </method>\n";
               }
            for (Class sig: c.getDeclaredClasses()) 
               if (DBusSignal.class.isAssignableFrom(sig)) {
                  introspectiondata += "  <signal name=\""+sig.getSimpleName()+"\">\n";
                  Constructor con = sig.getConstructors()[0];
                  //Constructor con = org.freedesktop.dbus.test.TestSignalInterface.TestSignal.class.getConstructors()[0];
                  Type[] ts = con.getGenericParameterTypes();
                  for (int j = 1; j < ts.length; j++)
                     introspectiondata += "   <arg type=\""+DBusConnection.getDBusType(ts[j])+"\" direction=\"out\" />\n";
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
      methods = new HashMap<MethodTuple,Method>();
      introspectiondata = "";
      methods.putAll(getExportedMethods(object.getClass()));
      introspectiondata += 
         " <interface name=\"org.freedesktop.DBus.Introspectable\">\n"+
         "  <method name=\"Introspect\">\n"+
         "   <arg type=\"s\" direction=\"out\"/>\n"+
         "  </method>\n"+
         " </interface>\n";
   }
}

/** Handles a connection to DBus.
 * <p>
 * This is a Singleton class, only 1 connection to the SYSTEM or SESSION busses can be made.
 * Repeated calls to getConnection will return the same reference.
 * </p>
 * <p>
 * Signal Handlers and method calls from remote objects are run in their own threads, you MUST handle the concurrency issues.
 * </p>
 */
public class DBusConnection 
{
   private class _sighandler implements DBusSigHandler
   {
      public void handle(DBusSignal s)
      {
         if (s instanceof org.freedesktop.DBus.Local.Disconnected) {
            DBusErrorMessage err = new DBusErrorMessage(
                  servicenames.get(0), servicenames.get(0), 
                  "org.freedesktop.DBus.Local.Disconnected",
                  new Object[] { "Disconnected" }, 0, 0);
            synchronized (pendingCalls) {
               Long[] set = (Long[]) pendingCalls.keySet().toArray(new Long[]{});
               for (Long l: set) {
                  MethodCall m = pendingCalls.get(l);
                  pendingCalls.remove(l);
                  m.setReply(err);
               }
            }
            synchronized (pendingErrors) {
               pendingErrors.add(err);
            }
         }
      }
   }
   private class _thread extends Thread
   {
      public _thread()
      {
         setName("DBusConnection");
      }
      public void run()
      {
         DBusMessage m = null;
         while (_run) {
            m = null;

            // read from the wire
            m = readIncoming(TIMEOUT);
            if (m != null) {
               synchronized (this) { notifyAll(); }

               if (m instanceof DBusSignal)
                  handleMessage((DBusSignal) m);
               else if (m instanceof MethodCall)
                  handleMessage((MethodCall) m);
               else if (m instanceof MethodReply)
                  handleMessage((MethodReply) m);
               else if (m instanceof DBusErrorMessage)
                  handleMessage((DBusErrorMessage) m);

               m = null;
            }

            // write to the wire
            synchronized (outgoing) {
               if (outgoing.size() > 0)
                  m = outgoing.removeFirst(); }
            while (null != m) {
               sendMessage(m);
               m = null;
               synchronized (outgoing) {
                  if (outgoing.size() > 0)
                     m = outgoing.removeFirst(); }
            }
         }
         synchronized (outgoing) {
            if (outgoing.size() > 0)
               m = outgoing.removeFirst(); 
         }
         while (null != m) {
            sendMessage(m);
            m = null;
            synchronized (outgoing) {
               if (outgoing.size() > 0)
                  m = outgoing.removeFirst(); }
         }
         synchronized (this) { notifyAll(); }
      }
   }
   private class _globalhandler implements org.freedesktop.DBus.Peer, org.freedesktop.DBus.Introspectable
   {
      private String objectpath;
      public _globalhandler()
      {
         this.objectpath = null;
      }
      public _globalhandler(String objectpath)
      {
         this.objectpath = objectpath;
      }
      public boolean isRemote() { return false; }
      public void Ping() { return; }
      public String Introspect() 
      {
         if ("/".equals(objectpath)) {
            String data = "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\" "+
         "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n"+
         "<node name=\"/\">\n";
            for (String path: exportedObjects.keySet()) {
               if (null == path) continue;
               data += "<node name=\""+path+"\">\n"
                  + exportedObjects.get(path).introspectiondata
                  + "</node>\n";
            }
            data += "</node>";
            return data;
         }
         ExportedObject eo = exportedObjects.get(objectpath);
         if (null == eo) 
            throw new DBusExecutionException("Introspecting on non-existant object");
         else return 
            "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\" "+
         "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n"+
         "<node name=\""+objectpath+"\">\n"+
            eo.introspectiondata +
         "</node>";
      }
   }
   /**
    * System Bus
    */
   public static final int SYSTEM = 0;
   /**
    * Session Bus
    */
   public static final int SESSION = 1;
   /**
    * Timeout in ms on checking the BUS for incoming messages and sending outgoing messages
    */
   private static final int TIMEOUT = 100;

   static final String SERVICE_REGEX = "^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$";
   static final String OBJECT_REGEX = "^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$";

   private Map<String,ExportedObject> exportedObjects;
   private Map<DBusInterface,String> importedObjects;
   private Map<SignalTuple,Vector<DBusSigHandler>> handledSignals;
   private Map<Long,MethodCall> pendingCalls;
   private Vector<String> servicenames;
   private boolean _run;
   private int connid;
   LinkedList<DBusMessage> outgoing;
   LinkedList<DBusErrorMessage> pendingErrors;

   private native int dbus_connect(int bustype) throws DBusException;
   private native int dbus_connect(String address) throws DBusException;
   private native void dbus_disconnect(int connid);
   private native void dbus_listen_signal(int connid, String type, String name) throws DBusException;
   private native DBusMessage dbus_read_write_pop(int connid, int timeoutms);
   private native int dbus_send_signal(int connid, String objectpath, String type, String name, Object... parameters);
   private native int dbus_send_error_message(int connid, String destination, String name, long replyserial, Object... params);
   private native int dbus_call_method(int connid, String service, String objectpath, String type, String name, Object... params);
   private native int dbus_reply_to_call(int connid, String destination, String type, String objectpath, String name, long replyserial, Object... params);
   static {
      System.loadLibrary("dbus-1");
      System.loadLibrary("dbus-java");
   }
   private static final Map<Object,DBusConnection> conn = new HashMap<Object,DBusConnection>();
   private int _refcount = 0;
   private Object _reflock = new Object();
   private Object connkey;
   private DBus _dbus;
   private _thread thread;

   /**
    * Connect to the BUS. If a connection already exists to the specified Bus, a reference to it is returned.
    * @param address The address of the bus to connect to
    * @throws DBusException  If there is a problem connecting to the Bus.
    */
   public static DBusConnection getConnection(String address) throws DBusException
   {
      synchronized (conn) {
         DBusConnection c = conn.get(address);
         if (null != c) {
            synchronized (c._reflock) { c._refcount++; }
            return c;
         }
         else {
            c = new DBusConnection(address);
            conn.put(address, c);
            return c;
         }
      }
   }
   /**
    * Connect to the BUS. If a connection already exists to the specified Bus, a reference to it is returned.
    * @param bustype The Bus to connect to.
    * @see #SYSTEM
    * @see #SESSION
    * @throws DBusException  If there is a problem connecting to the Bus.
    */
   public static DBusConnection getConnection(int bustype) throws DBusException
   {
      synchronized (conn) {
         if (bustype > 1 || bustype < 0) throw new DBusException("Invalid Bus Specifier");
         DBusConnection c = conn.get(bustype);
         if (null != c) {
            synchronized (c._reflock) { c._refcount++; }
            return c;
         }
         else {
            c = new DBusConnection(bustype);
            conn.put(bustype, c);
            return c;
         }
      }
   }
   /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    * @param c The Java type.
    * @return The DBus type.
    * @throws DBusException If the given type cannot be converted to a DBus type.
    */
   public static String getDBusType(Type c) throws DBusException
   {
      return getDBusType(c, false);
   }
   /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    * @param c The Java type.
    * @param basic If true enforces this to be a non-compound type. (compound types are Maps, Structs and Lists/arrays).
    * @return The DBus type.
    * @throws DBusException If the given type cannot be converted to a DBus type.
    */
   public static String getDBusType(Type c, boolean basic) throws DBusException
   {
      StringBuffer out = new StringBuffer();

      if (basic && !(c instanceof Class))
         throw new DBusException(c+" is not a basic type");

      if (c instanceof TypeVariable) out.append('v');
      else if (c instanceof GenericArrayType) {
         out.append('a');
         out.append(getDBusType(((GenericArrayType) c).getGenericComponentType()));
      }
      else if (c instanceof ParameterizedType) {
         ParameterizedType p = (ParameterizedType) c;
         if (p.getRawType().equals(Map.class)) {
            out.append("a{");
            Type[] t = p.getActualTypeArguments();
            try {
               out.append(getDBusType(t[0], true));
               out.append(getDBusType(t[1], false));
            } catch (ArrayIndexOutOfBoundsException AIOOBe) {
               throw new DBusException("Map must have 2 parameters");
            }
            out.append('}');
         }
         else if (List.class.isAssignableFrom((Class) p.getRawType())) {
            out.append('a');
            for (Type t: p.getActualTypeArguments())
               out.append(getDBusType(t, false));
         } else if (Struct.class.isAssignableFrom((Class) p.getRawType())) {
            out.append('(');
            for (Type t: p.getActualTypeArguments())
               out.append(getDBusType(t, false));
            out.append(')');
         }
         else if (p.getRawType().equals(Variant.class)) {
            out.append('v');
         }
         else if (DBusInterface.class.isAssignableFrom((Class) p.getRawType())) {
            out.append('o');
         }
         else
            throw new DBusException("Exporting non-exportable parameterized type "+c);
      }
      
      else if (c.equals(Byte.class)) out.append('y');
      else if (c.equals(Byte.TYPE)) out.append('y');
      else if (c.equals(Boolean.class)) out.append('b');
      else if (c.equals(Boolean.TYPE)) out.append('b');
      else if (c.equals(Short.class)) out.append('n');
      else if (c.equals(Short.TYPE)) out.append('n');
      else if (c.equals(UInt16.class)) out.append('q');
      else if (c.equals(Integer.class)) out.append('i');
      else if (c.equals(Integer.TYPE)) out.append('i');
      else if (c.equals(UInt32.class)) out.append('u');
      else if (c.equals(Long.class)) out.append('x');
      else if (c.equals(Long.TYPE)) out.append('x');
      else if (c.equals(UInt64.class)) out.append('t');
      else if (c.equals(Double.class)) out.append('d');
      else if (c.equals(Double.TYPE)) out.append('d');
      else if (c.equals(String.class)) out.append('s');
      else if (c.equals(Variant.class)) out.append('v');
      else if (c instanceof Class && 
            DBusInterface.class.isAssignableFrom((Class) c)) out.append('o');
      else if (c instanceof Class && ((Class) c).isArray()) {
         out.append('a');
         out.append(getDBusType(((Class) c).getComponentType(), false));
      }
      else {
         throw new DBusException("Exporting non-exportable type "+c);
      }

      return out.toString();
   }
   /**
    * Converts a dbus type string into a Java type name, 
    * putting any needed import lines into imports.
    * If you set container to be true then no primative types will
    * be returned. 
    * @param dbus The DBus type.
    * @param imports This is populated with any required imports lines.
    * @param structs This is populated with any structs which should be created.
    * @param container Indicates this is a container type and no primitive types should be used.
    * @param fullnames Will return fully qualified type names if true.
    */
   public static String getJavaType(String dbus, Set<String> imports, Map<String,Integer> structs, boolean container, boolean fullnames) throws DBusException
   {
      if (null == dbus || "".equals(dbus)) return "";
      
      try {
         switch(dbus.charAt(0)) {
            case '(':
               int elements = 0;
               String types="<";
               String s = dbus.substring(1, dbus.length()-1);
               for (int i = 0; i < s.length(); i++) {
                  switch (s.charAt(i)) {
                     case 'a':
                        if ('{' == s.charAt(i+1)) {
                           int j, c;
                           for (j = i+2, c = 1; c > 0; j++)
                              if ('{' == s.charAt(j)) c++; 
                              else if ('}' == s.charAt(j)) c--;
                           types += getJavaType(s.substring(i,j+1), imports, structs, true, false) +  ", ";
                           i=j;
                        } else {
                           types += getJavaType(s.substring(i,i+2), imports, structs, true, false) + ", ";
                           i++;
                        }
                        elements++;
                        break;
                        
                     case '(':
                        int j, c;
                        for (j = i+1, c = 1; c > 0; j++)
                           if ('(' == s.charAt(j)) c++;
                           else if (')' == s.charAt(j)) c--;
                        types += getJavaType(s.substring(i,j+1), imports, structs, true, false) +  ", ";                    
                        elements++;
                        i=j;
                        break;

                     default:
                        types += getJavaType(s.substring(i,i+1), imports, structs, true, false) + ", ";
                        elements++;
                  }
               }
               String name = "Struct"+elements;
               if (null != structs) structs.put(name, elements);
               if (fullnames) return "org.freedesktop.dbus.Struct";
               else return name + types.replaceAll(", $", ">");
            case 'a':
               if ('{' == dbus.charAt(1)) {
                  if (null != imports) imports.add("java.utils.Map");
                  if (fullnames) return "java.util.Map";
                  else return "Map<"+getJavaType(dbus.substring(2,3), imports, structs, true, false)+", "+
                     getJavaType(dbus.substring(3,dbus.length()-1), imports, structs, true, false)+">";
               }
               if (null != imports) imports.add("java.utils.List");
               if (fullnames) return "java.util.List";
               else return "List<"+getJavaType(dbus.substring(1), imports, structs, true, false) + ">";
            case 'v':
               if (null != imports) imports.add("org.freedesktop.dbus.Variant");
               if (fullnames) return "org.freedesktop.dbus.Variant";
               else return "Variant";
            case 'b':
               if (container) {
                  if (fullnames) return "java.lang.Boolean";
                  else return "Boolean";
               }
               else return "boolean";
            case 'n':
               if (container) {
                  if (fullnames) return "java.lang.Short";
                  else return "Short";
               } else return "short";
            case 'y':
               if (container) {
                  if (fullnames) return "java.lang.Byte";
                  else return "Byte";
               } else return "byte";
            case 'o':
               if (null != imports) imports.add("org.freedesktop.dbus.DBusInterface");
               if (fullnames) return "org.freedesktop.dbus.DBusInterface";
               else return "DBusInterface";
            case 'q':
               if (null != imports) imports.add("org.freedesktop.dbus.UInt16");
               if (fullnames) return "org.freedesktop.dbus.UInt16";
               else return "UInt16";
            case 'i':
               if (container) {
                  if (fullnames) return "java.lang.Integer";
                  else return "Integer";
               } else return "int";
            case 'u':
               if (null != imports) imports.add("org.freedesktop.dbus.UInt32");
               if (fullnames) return "org.freedesktop.dbus.UInt32";
               else return "UInt32";
            case 'x':
               if (container) {
                  if (fullnames) return "java.lang.Long";
                  else return "Long";
               } else return "long";
            case 't':
               if (null != imports) imports.add("org.freedesktop.dbus.UInt64");
               if (fullnames) return "org.freedesktop.dbus.UInt64";
               else return "UInt64";
            case 'd':
               if (container) {
                  if (fullnames) return "java.lang.Double";
                  else return "Double";
               } else return "double";
            case 's':
               if (fullnames) return "java.lang.String";
               else return "String";
            default:
               throw new DBusException("Failed to parse DBus type signature: "+dbus);
         }
      } catch (IndexOutOfBoundsException IOOBe) {
         throw new DBusException("Failed to parse DBus type signature: "+dbus);
      }
   }
   static Object[] convertParameters(Object[] parameters, Type[] types) throws Exception
   {
      if (null == parameters) return null;
      for (int i = 0; i < parameters.length; i++) {
         
         // its an unwrapped variant, wrap it
         if (types[i] instanceof TypeVariable &&
               !(parameters[i] instanceof Variant)) {
            parameters[i] = new Variant(parameters[i]);
         }
         
         // its something parameterised
         else if (types[i] instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) types[i];
            Class r = (Class) p.getRawType();

            // its a list, delist it
            if (List.class.isAssignableFrom(r)) {
               Class contained = (Class) p.getActualTypeArguments()[0];
               parameters[i] = ArrayFrob.delist((List) parameters[i], contained);
            }
            // its a map, wrap it in our typed container class
            else if (Map.class.isAssignableFrom(r)) {
               parameters[i] = new MapContainer((Map) parameters[i], p);
            }
            // its a struct, recurse over it
            else if (Struct.class.isAssignableFrom(r)) {
               Constructor con = r.getDeclaredConstructors()[0];
               Object[] oldparams = ((Struct) parameters[i]).getParameters();
               Type[] ts = p.getActualTypeArguments();
               Object[] newparams = convertParameters(oldparams, ts);
               parameters[i] = con.newInstance(newparams);
            }
         }
         else if (types[i] instanceof GenericArrayType &&
               ! (parameters[i] instanceof Object[]))
            parameters[i] = ArrayFrob.wrap(parameters[i]);
         else if (types[i] instanceof Class &&
               ((Class) types[i]).isArray() &&
               ! (parameters[i] instanceof Object[]))
            parameters[i] = ArrayFrob.wrap(parameters[i]);
         
      }
      return parameters;
   }
   static Object[] deSerialiseParameters(Object[] parameters, Type[] types) throws Exception
   {
      if (null == parameters) return null;
      for (int i = 0; i < parameters.length; i++) {
         
         // its a wrapped variant, unwrap it
         if (types[i] instanceof TypeVariable &&
               parameters[i] instanceof Variant) {
            parameters[i] = ((Variant)parameters[i]).getValue();
         }

         // its a wrapped map, unwrap it
         if (parameters[i] instanceof MapContainer)
            parameters[i] = ((MapContainer) parameters[i]).getMap(types[i]);

         // it should be a struct. create it
         if (parameters[i] instanceof Object[] && 
               types[i] instanceof ParameterizedType &&
               Struct.class.isAssignableFrom((Class) ((ParameterizedType) types[i]).getRawType())) {
            Constructor con = ((Class) ((ParameterizedType) types[i]).getRawType()).getConstructors()[0];
            Type[] gpts = con.getGenericParameterTypes();
            Type[] ts = new Type[gpts.length];
            for (int j = 0; j < gpts.length; j++)
               ts[j] = ((TypeVariable) gpts[j]).getBounds()[0];
               
            // recurse over struct contents
            parameters[i] = deSerialiseParameters((Object[]) parameters[i], ts);
            parameters[i] = con.newInstance((Object[]) parameters[i]);
         }

         // recurse over arrays
         if (parameters[i] instanceof Object[]) {
            Type[] ts = new Type[((Object[]) parameters[i]).length];
            Arrays.fill(ts, parameters[i].getClass().getComponentType());
            parameters[i] = deSerialiseParameters((Object[]) parameters[i],
                  ts);
         }

         // make sure arrays are in the correct format
         if ((parameters[i] instanceof Object[] ||
                  parameters[i] instanceof List)) {
            if (types[i] instanceof ParameterizedType)
               parameters[i] = ArrayFrob.convert(parameters[i],
                     (Class) ((ParameterizedType) types[i]).getRawType());
            else if (types[i] instanceof GenericArrayType) {
               Type ct = ((GenericArrayType) types[i]).getGenericComponentType();
               Class cc = null;
               if (ct instanceof Class)
                  cc = (Class) ct;
               if (ct instanceof ParameterizedType)
                  cc = (Class) ((ParameterizedType) ct).getRawType();
               Object o = Array.newInstance(cc, 0);
               parameters[i] = ArrayFrob.convert(parameters[i],
                     o.getClass());
            } else if (types[i] instanceof Class &&
                  ((Class) types[i]).isArray()) {
               Class cc = ((Class) types[i]).getComponentType();
               Object o = Array.newInstance(cc, 0);
               parameters[i] = ArrayFrob.convert(parameters[i],
                     o.getClass());
            }
         }
      }
      return parameters;
   }

   private DBusConnection() throws DBusException
   {
      exportedObjects = new HashMap<String,ExportedObject>();
      importedObjects = new HashMap<DBusInterface,String>();
      exportedObjects.put(null, new ExportedObject(new _globalhandler()));
      handledSignals = new HashMap<SignalTuple,Vector<DBusSigHandler>>();
      pendingCalls = new HashMap<Long,MethodCall>();
      servicenames = new Vector<String>();
      outgoing = new LinkedList<DBusMessage>();
      pendingErrors = new LinkedList<DBusErrorMessage>();
      _run = true;
      synchronized (_reflock) {
         _refcount = 1; 
      }
   }
   private DBusConnection(String address) throws DBusException
   {
      this();
      connkey = address;

      connid = dbus_connect(address);

      // start listening
      thread = new _thread();
      thread.start();
      
      // register ourselves
      _dbus = (DBus) getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
      try {
         servicenames.add(_dbus.Hello());
      } catch (DBusExecutionException DBEe) {
         throw new DBusException(DBEe.getMessage());
      }
      // register disconnect handlers
      addSigHandler(org.freedesktop.DBus.Local.Disconnected.class, new _sighandler());
   }


   private DBusConnection(int bustype) throws DBusException
   {
      this();
      connkey = bustype;

      connid = dbus_connect(bustype);

      // start listening
      thread = new _thread();
      thread.start();
      
      // register ourselves
      _dbus = (DBus) getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
      try {
         servicenames.add(_dbus.Hello());
      } catch (DBusExecutionException DBEe) {
         throw new DBusException(DBEe.getMessage());
      }
      // register disconnect handlers
      addSigHandler(org.freedesktop.DBus.Local.Disconnected.class, new _sighandler());
   }

   String getExportedObject(DBusInterface i) throws DBusException
   {
      for (String s: exportedObjects.keySet())
         if (exportedObjects.get(s).object.equals(i))
            return s;

      String s = importedObjects.get(i);
      if (null != s) return s;

      throw new DBusException("Not an object exported or imported by this connection"); 
   }

   DBusInterface getExportedObject(String s) throws DBusException
   {
      ExportedObject o = exportedObjects.get(s);
      if (null == o) throw new DBusException("Not an object exported by this connection");
      else return o.object;
   }


   /** 
    * Register a service.
    * Register the well known name that this should respond to on the Bus.
    * @param servicename The name to respond to. MUST be in dot-notation like "org.freedesktop.local"
    * @throws DBusException If the register name failed, or our name already exists on the bus.
    *  or if servicename is incorrectly formatted.
    */
   public void registerService(String servicename) throws DBusException
   {
      if (!servicename.matches(SERVICE_REGEX)) throw new DBusException("Invalid service name");
      synchronized (this.servicenames) {
         UInt32 rv;
         try { 
            rv = _dbus.RequestName(servicename, 
                  new UInt32(DBus.DBUS_NAME_FLAG_REPLACE_EXISTING |
                     DBus.DBUS_NAME_FLAG_DO_NOT_QUEUE));
         } catch (DBusExecutionException DBEe) {
            throw new DBusException(DBEe.getMessage());
         }
         switch (rv.intValue()) {
            case DBus.DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER: break;
            case DBus.DBUS_REQUEST_NAME_REPLY_IN_QUEUE: throw new DBusException("Failed to register service name");
            case DBus.DBUS_REQUEST_NAME_REPLY_EXISTS: throw new DBusException("Failed to register service name");
            case DBus.DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER: break;
            default: break;
         }
         this.servicenames.add(servicename);
      }
   }
   /** 
    * Export an object so that its methods can be called on DBus.
    * @param objectpath The path to the object we are exposing. MUST be in slash-notation, like "org/freedesktop/Local", 
    * and SHOULD end with a capitalised term. Only one object may be exposed on each path at any one time, but an object
    * may be exposed on several paths at once.
    * @param object The object to export.
    * @throws DBusException If the objectpath is already exporting an object.
    *  or if objectpath is incorrectly formatted,
    */
   public void exportObject(String objectpath, DBusInterface object) throws DBusException
   {
      if (!objectpath.matches(OBJECT_REGEX)) throw new DBusException("Invalid object path");
      if (null == objectpath || "".equals(objectpath)) 
         throw new DBusException("Must Specify an Object Path");
      synchronized (exportedObjects) {
         if (null != exportedObjects.get(objectpath)) 
            throw new DBusException("Object already exported");
         exportedObjects.put(objectpath, new ExportedObject(object));
      }
   }
   /** 
    * Stop Exporting an object 
    * @param objectpath The objectpath to stop exporting.
    */
   public void unExportObject(String objectpath) 
   {
      synchronized (exportedObjects) {
         exportedObjects.remove(objectpath);
      }
   }
   /** 
    * Return a reference to a remote object 
    * @param service The service to connect to. Usually a well known service name in dot-notation (such as "org.freedesktop.local")
    * or may be a DBus address such as ":1-16".
    * @param objectpath The path on which the service is exporting the object.
    * @param type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
    * as the interface the remote object is exporting.
    * @return A reference to a remote object.
    * @throws ClassCastException If type is not a sub-type of DBusInterface
    * @throws DBusException If service or objectpath are incorrectly formatted.
    */
   public DBusInterface getRemoteObject(String service, String objectpath, Class<? extends DBusInterface> type) throws DBusException
   {
      if (!service.matches(SERVICE_REGEX)) throw new DBusException("Invalid service name");
      if (!objectpath.matches(OBJECT_REGEX)) throw new DBusException("Invalid object path");
      if (!DBusInterface.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Interface");
      RemoteObject ro = new RemoteObject(service, objectpath, type.getName().replaceAll("[$]","."));
      DBusInterface i =  (DBusInterface) Proxy.newProxyInstance(type.getClassLoader(), 
            new Class[] { type }, new RemoteInvocationHandler(this, ro, type));
      importedObjects.put(i, objectpath);
      return i;
   }
   /** 
    * Send a signal.
    * @param signal The signal to send.
    */
   public void sendSignal(DBusSignal signal)
   {
      synchronized (outgoing) {
         outgoing.addLast(signal); }
   }
   /** 
    * Add a Signal Handler.
    * Adds a signal handler to call when a signal is received which matches the specified type and name.
    * @param type The signal to watch for. 
    * @param handler The handler to call when a signal is received.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public void addSigHandler(Class<? extends DBusSignal> type, DBusSigHandler handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      addSigHandler(new DBusMatchRule(type), handler);
   }
   private void addSigHandler(DBusMatchRule rule, DBusSigHandler handler) throws DBusException
   {
         try {
            _dbus.AddMatch(rule.toString());
         } catch (DBusExecutionException DBEe) {
            throw new DBusException(DBEe.getMessage());
         }
         SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember());
      synchronized (handledSignals) {
         Vector<DBusSigHandler> v = handledSignals.get(key);
         if (null == v) {
            v = new Vector<DBusSigHandler>();
            v.add(handler);
            handledSignals.put(key, v);
         } else
            v.add(handler);
      }
   }
   /** 
    * Disconnect from the Bus.
    * This only disconnects when the last reference to the bus has disconnect called on it
    * or has been destroyed.
    */
   public void disconnect()
   {
      synchronized (conn) {
         synchronized (_reflock) {
            if (0 == --_refcount) {
               _run = false;
               try {
                  synchronized (thread) { thread.wait(); }
               } catch (InterruptedException Ie) {}
               dbus_disconnect(connid);
               conn.remove(connkey);
            }
         }
      }
   }
   public void finalize()
   {
      disconnect();
   }
   /**
    * Return any DBus error which has been received.
    * @return A DBusExecutionException, or null if no error is pending.
    */
   public DBusExecutionException getError()
   {
      synchronized (pendingErrors) {
         if (pendingErrors.size() == 0) return null;
         else 
            return pendingErrors.removeFirst().getException();
      }
   }
   
   private void handleMessage(final MethodCall m) 
   {
      // get the method signature
      Object[] params = m.getParameters();

      ExportedObject eo = null;
      Method meth = null;
      Object o;
      
      synchronized (exportedObjects) {
         eo = exportedObjects.get(null);
      }
      if (null != eo) {
         meth = eo.methods.get(new MethodTuple(m.getType(), m.getName()));
      }
      if (null != meth)
         o = new _globalhandler(m.getObjectPath());

      else {
         // now check for specific exported functions

         synchronized (exportedObjects) {
            eo = exportedObjects.get(m.getObjectPath());
         }

         if (null == eo) {
            synchronized (outgoing) {
               outgoing.addLast(new DBusErrorMessage(m, new DBus.Error.UnknownObject(m.getObjectPath()+" is not an object provided by this service."))); }
            return;
         }
         meth = eo.methods.get(new MethodTuple(m.getType(), m.getName()));
         if (null == meth) {
            synchronized (outgoing) {
               outgoing.addLast(new DBusErrorMessage(m, new DBus.Error.UnknownMethod("The method `"+m.getType()+"."+m.getName()+"' does not exist on this object."))); }
            return;
         }
         o = eo.object;
      }

      try {
         Type[] ts = meth.getGenericParameterTypes();
         m.parameters = deSerialiseParameters(m.parameters, ts);
      } catch (Exception e) {
         synchronized (outgoing) {
            outgoing.addLast(new DBusErrorMessage(m, new DBus.Error.UnknownMethod("Failure in de-serialising message ("+e+")"))); }
      }

      // now execute it
      final Method me = meth;
      final Object ob = o;
      final LinkedList<DBusMessage> outqueue = outgoing;
      new Thread() 
      { 
         public void run() 
         { 
            try { 
               Object result;
               try {
                  result = me.invoke(ob, m.parameters);
               } catch (InvocationTargetException ITe) {
                  throw ITe.getCause();
               }
               result = convertParameters(new Object[] { result }, new Type[] { me.getGenericReturnType() })[0];
               MethodReply reply = new MethodReply(m, result);
               synchronized (outqueue) {
                  outqueue.addLast(reply);
               }
            } catch (DBusExecutionException DBEe) {
               synchronized (outqueue) {
                  outqueue.addLast(new DBusErrorMessage(m, DBEe)); 
               }
            } catch (Throwable e) {
               synchronized (outqueue) {
                  outqueue.addLast(new DBusErrorMessage(m, new DBusExecutionException("Error Executing Method "+m.getType()+"."+m.getName()+": "+e.getMessage()))); 
               }
            } 
         }
      }.start();
   }

   private void handleMessage(final DBusSignal s)
   {
      final Vector<DBusSigHandler> v;
      synchronized(handledSignals) {
         v = handledSignals.get(new SignalTuple(s.getType(), s.getName()));
      }
      if (null == v) return;
      new Thread() { public void run() {
         for (DBusSigHandler h: v)
            h.handle(s); 
      } }.start();
   }
   private void handleMessage(final DBusErrorMessage err)
   {
      MethodCall m;
      synchronized (pendingCalls) {
         m = pendingCalls.get(err.getReplySerial());
         if (null != m)
            pendingCalls.remove(err.getReplySerial());
      }
      if (null != m)
         m.setReply(err);
      else
         synchronized (pendingErrors) {
            pendingErrors.addLast(err); }
   }
   private void handleMessage(final MethodReply mr)
   {
      MethodCall m;
      synchronized (pendingCalls) {
         m = pendingCalls.get(mr.getReplySerial());
         if (null != m)
            pendingCalls.remove(mr.getReplySerial());
      }
      if (null != m)
         m.setReply(mr);
      else
         synchronized (outgoing) {
            outgoing.addLast(new DBusErrorMessage(mr, new DBusExecutionException("Spurious reply. No message with the given serial id was awaiting a reply."))); 
         }
   }
   private void sendMessage(DBusMessage m)
   {
      if (m instanceof DBusSignal) 
         try {
            m.setSerial(dbus_send_signal(connid, ((DBusSignal) m).getObjectPath(), m.getType(), m.getName(), m.getParameters()));
         } catch (Exception e) {}
      else if (m instanceof DBusErrorMessage) 
         try {
            m.setSerial(dbus_send_error_message(connid, ((DBusErrorMessage) m).getDestination(), m.getType(), m.getReplySerial(), m.getParameters()));
         } catch (Exception e) {}
      else if (m instanceof MethodCall) {
         try {
            synchronized (pendingCalls) {
               m.setSerial(dbus_call_method(connid, ((MethodCall) m).getService(), ((MethodCall) m).getObjectPath(), m.getType(), m.getName(), m.getParameters()));
               if (0 < m.getSerial()) {
                  pendingCalls.put(m.getSerial(),(MethodCall) m);
               }
               else
                  ((MethodCall) m).setReply(new DBusErrorMessage(m, new InternalMessageException("Message Failed to Send")));
            }
         } catch (DBusExecutionException DBEe) {
               ((MethodCall) m).setReply(new DBusErrorMessage(m, DBEe));
         } catch (Exception e) {
               ((MethodCall) m).setReply(new DBusErrorMessage(m, new DBusExecutionException("Message Failed to Send: "+e.getMessage())));
         }
      }
      else if (m instanceof MethodReply) {
         MethodCall call = ((MethodReply) m).getCall();
         try {
            m.setSerial(dbus_reply_to_call(connid, call.getSource(), call.getType(), call.getObjectPath(), call.getName(), call.getSerial(), m.getParameters()));
         } catch (Exception e) {
            dbus_send_error_message(connid, call.getSource(), DBusExecutionException.class.getName(), call.getSerial(), new Object[] { "Error sending reply: "+e.getMessage() });
         }
      }
   }
   private DBusMessage readIncoming(int timeoutms)
   {
      DBusMessage m = dbus_read_write_pop(connid, timeoutms);
      return m;
   }
}
