package org.freedesktop.dbus;

import java.lang.annotation.Annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

import java.util.regex.Pattern;

import org.freedesktop.DBus;

class SignalTuple
{
   String type;
   String name;
   String object;
   String source;
   public SignalTuple(String type, String name, String object, String source)
   {
      this.type = type;
      this.name = name;
      this.object = object;
      this.source = source;
   }
   public boolean equals(Object o)
   {
      if (!(o instanceof SignalTuple)) return false;
      SignalTuple other = (SignalTuple) o;
      if (null == this.type && null != other.type) return false;
      if (null != this.type && !this.type.equals(other.type)) return false;
      if (null == this.name && null != other.name) return false;
      if (null != this.name && !this.name.equals(other.name)) return false;
      if (null == this.object && null != other.object) return false;
      if (null != this.object && !this.object.equals(other.object)) return false;
      if (null == this.source && null != other.source) return false;
      if (null != this.source && !this.source.equals(other.source)) return false;
      return true;
   }
   public int hashCode()
   {
      return (null == type ? 0 : type.hashCode())
         +   (null == name ? 0 : name.hashCode())
         +   (null == source ? 0 : source.hashCode())
         +   (null == object ? 0 : object.hashCode());
   }
}

class MethodTuple
{
   String name;
   String sig;
   public MethodTuple(String name, String sig)
   {
      this.name = name;
      this.sig = sig;
   }
   public boolean equals(Object o)
   {
      return o.getClass().equals(MethodTuple.class)
            && ((MethodTuple) o).name.equals(this.name)
            && ((MethodTuple) o).sig.equals(this.sig);
   }
   public int hashCode()
   {
      return name.hashCode()+sig.hashCode();
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
            // add this class's public methods
            if (c.getName().length() > DBusConnection.MAX_NAME_LENGTH) 
               throw new DBusException("Introspected interface name exceeds 255 characters. Cannot export objects of type "+c.getName()+".");
            introspectiondata += " <interface name=\""+c.getName()+"\">\n";
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
                        for (Type t: ((ParameterizedType) meth.getGenericReturnType()).getActualTypeArguments())
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
                  busnames.get(0), busnames.get(0), 
                  "org.freedesktop.DBus.Local.Disconnected", "s",
                  new Object[] { "Disconnected" }, 0, 0);
            synchronized (pendingCalls) {
               long[] set = pendingCalls.getKeys();
               for (long l: set) if (-1 != l) {
                  MethodCall m = pendingCalls.remove(l);
                  if (null != m)
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
         try {
            DBusMessage m = null;
            while (_run) {
               m = null;

               // read from the wire
               try {
                  // this blocks on outgoing being non-empty or a message being available.
                  m = readIncoming(TIMEOUT, outgoing);
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
               } catch (Exception e) { }

               // write to the wire
               synchronized (outgoing) {
                  if (!outgoing.isEmpty())
                     m = outgoing.remove(); }
               while (null != m) {
                  sendMessage(m);
                  m = null;
                  synchronized (outgoing) {
                     if (!outgoing.isEmpty())
                        m = outgoing.remove(); }
               }
            }
            synchronized (outgoing) {
               if (!outgoing.isEmpty())
                  m = outgoing.remove(); 
            }
            while (null != m) {
               sendMessage(m);
               m = null;
               synchronized (outgoing) {
                  if (!outgoing.isEmpty())
                     m = outgoing.remove(); }
            }
            synchronized (this) { notifyAll(); }
         } catch (NotConnected NC) {}
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
         String intro =  objectTree.Introspect(objectpath);
         if (null == intro) 
            throw new DBus.Error.UnknownObject("Introspecting on non-existant object");
         else return 
            "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\" "+
               "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n"+intro;
      }
   }
   private class _workerthread extends Thread
   {
      private boolean _run = true;
      public void halt()
      {
         _run = false;
      }
      public void run()
      {
        while (_run) {
           Runnable r = null;
           synchronized (runnables) {
              while (runnables.size() == 0 && _run) 
                 try { runnables.wait(); } catch (InterruptedException Ie) {}
              if (runnables.size() > 0)
                 r = runnables.removeFirst();
           }
           if (null != r) r.run();
        }
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
   private static final int TIMEOUT = 1;
   /** Initial size of the pending calls map */
   private static final int PENDING_MAP_INITIAL_SIZE = 10;

   static final String BUSNAME_REGEX = "^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$";
   static final String CONNID_REGEX = "^:[0-9]*\\.[0-9]*$";
   static final String OBJECT_REGEX = "^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$";
   static final byte THREADCOUNT = 4;
   static final int MAX_ARRAY_LENGTH = 67108864;
   static final int MAX_NAME_LENGTH = 255;

   private Map<String,ExportedObject> exportedObjects;
   private ObjectTree objectTree;
   private Map<DBusInterface,RemoteObject> importedObjects;
   private Map<SignalTuple,Vector<DBusSigHandler>> handledSignals;
   private EfficientMap pendingCalls;
   private Vector<String> busnames;
   private LinkedList<Runnable> runnables;
   private LinkedList<_workerthread> workers;
   private boolean _run;
   private int connid;
   EfficientQueue outgoing;
   LinkedList<DBusErrorMessage> pendingErrors;

   private native int dbus_connect(int bustype) throws DBusException;
   private static native boolean get_exception_debug_state();
   private native int dbus_connect(String address) throws DBusException;
   private native void dbus_disconnect(int connid);
   private native void dbus_listen_signal(int connid, String type, String name) throws DBusException;
   private native DBusMessage dbus_read_write_pop(int connid, int timeoutms, EfficientQueue outgoing);
   private native int dbus_send_signal(int connid, String objectpath, String type, String name, Object... parameters);
   private native int dbus_send_error_message(int connid, String destination, String name, long replyserial, Object... params);
   private native int dbus_call_method(int connid, String busname, String objectpath, String type, String name, int flags, Object... params);
   private native int dbus_reply_to_call(int connid, String destination, String type, String objectpath, String name, long replyserial, Object... params);
   static {
      System.loadLibrary("dbus-1");
      System.loadLibrary("dbus-java");
   }
   private static final Map<Object,DBusConnection> conn = new HashMap<Object,DBusConnection>();
   private static final Map<Thread,DBusCallInfo> infomap = new HashMap<Thread,DBusCallInfo>();
   private int _refcount = 0;
   private Object _reflock = new Object();
   private Object connkey;
   private DBus _dbus;
   private _thread thread;
   static final Pattern dollar_pattern = Pattern.compile("[$]");
   public static final boolean EXCEPTION_DEBUG;
   static {
      EXCEPTION_DEBUG = get_exception_debug_state();
   }

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
   public static String[] getDBusType(Type c) throws DBusException
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
   public static String[] getDBusType(Type c, boolean basic) throws DBusException
   {
      return recursiveGetDBusType(c, basic, 0);
   }
   private static StringBuffer[] out = new StringBuffer[10];
   public static String[] recursiveGetDBusType(Type c, boolean basic, int level) throws DBusException
   {
      if (out.length <= level) {
         StringBuffer[] newout = new StringBuffer[out.length];
         System.arraycopy(out, 0, newout, 0, out.length);
      }
      if (null == out[level]) out[level] = new StringBuffer();
      else out[level].delete(0, out.length);      

      if (basic && !(c instanceof Class))
         throw new DBusException(c+" is not a basic type");

      if (c instanceof TypeVariable) out[level].append('v');
      else if (c instanceof GenericArrayType) {
         out[level].append('a');
         String[] s = recursiveGetDBusType(((GenericArrayType) c).getGenericComponentType(), false, level+1);
         if (s.length != 1) throw new DBusException("Multi-valued array types not permitted");
         out[level].append(s[0]);
      }
      else if (c instanceof ParameterizedType) {
         ParameterizedType p = (ParameterizedType) c;
         if (p.getRawType().equals(Map.class)) {
            out[level].append("a{");
            Type[] t = p.getActualTypeArguments();
            try {
               String[] s = recursiveGetDBusType(t[0], true, level+1);
               if (s.length != 1) throw new DBusException("Multi-valued array types not permitted");
               out[level].append(s[0]);
               s = recursiveGetDBusType(t[1], false, level+1);
               if (s.length != 1) throw new DBusException("Multi-valued array types not permitted");
               out[level].append(s[0]);
            } catch (ArrayIndexOutOfBoundsException AIOOBe) {
               if (DBusConnection.EXCEPTION_DEBUG) AIOOBe.printStackTrace();
               throw new DBusException("Map must have 2 parameters");
            }
            out[level].append('}');
         }
         else if (List.class.isAssignableFrom((Class) p.getRawType())) {
            out[level].append('a');
            for (Type t: p.getActualTypeArguments()) {
               String[] s = recursiveGetDBusType(t, false, level+1);
               if (s.length != 1) throw new DBusException("Multi-valued array types not permitted");
               out[level].append(s[0]);
            }
         } 
         else if (p.getRawType().equals(Variant.class)) {
            out[level].append('v');
         }
         else if (DBusInterface.class.isAssignableFrom((Class) p.getRawType())) {
            out[level].append('o');
         }
         else
            throw new DBusException("Exporting non-exportable parameterized type "+c);
      }
      
      else if (c.equals(Byte.class)) out[level].append('y');
      else if (c.equals(Byte.TYPE)) out[level].append('y');
      else if (c.equals(Boolean.class)) out[level].append('b');
      else if (c.equals(Boolean.TYPE)) out[level].append('b');
      else if (c.equals(Short.class)) out[level].append('n');
      else if (c.equals(Short.TYPE)) out[level].append('n');
      else if (c.equals(UInt16.class)) out[level].append('q');
      else if (c.equals(Integer.class)) out[level].append('i');
      else if (c.equals(Integer.TYPE)) out[level].append('i');
      else if (c.equals(UInt32.class)) out[level].append('u');
      else if (c.equals(Long.class)) out[level].append('x');
      else if (c.equals(Long.TYPE)) out[level].append('x');
      else if (c.equals(UInt64.class)) out[level].append('t');
      else if (c.equals(Double.class)) out[level].append('d');
      else if (c.equals(Double.TYPE)) out[level].append('d');
      else if (c.equals(String.class)) out[level].append('s');
      else if (c.equals(Variant.class)) out[level].append('v');
      else if (c instanceof Class && 
            DBusInterface.class.isAssignableFrom((Class) c)) out[level].append('o');
      else if (c instanceof Class && ((Class) c).isArray()) {
         out[level].append('a');
         String[] s = recursiveGetDBusType(((Class) c).getComponentType(), false, level+1);
         if (s.length != 1) throw new DBusException("Multi-valued array types not permitted");
         out[level].append(s[0]);
      } else if (c instanceof Class && 
            Struct.class.isAssignableFrom((Class) c)) {
         out[level].append('(');
         Type[] ts = Struct.getStructTypeCache(c);
         if (null == ts) {
            Field[] fs = ((Class) c).getDeclaredFields();
            ts = new Type[fs.length];
            for (Field f : fs) {
               Position p = f.getAnnotation(Position.class);
               if (null == p) continue;
               ts[p.value()] = f.getGenericType();
           }
            Struct.putStructTypeCache(c, ts);
         }

         for (Type t: ts)
            if (t != null)
               for (String s: recursiveGetDBusType(t, false, level+1))
                  out[level].append(s);
         out[level].append(')');
      } else if ((c instanceof Class && 
            DBusSerializable.class.isAssignableFrom((Class) c)) ||
            (c instanceof ParameterizedType &&
             DBusSerializable.class.isAssignableFrom((Class) ((ParameterizedType) c).getRawType()))) {
         // it's a custom serializable type
         Type[] newtypes = null;
         if (c instanceof Class)  {
            for (Method m: ((Class) c).getDeclaredMethods()) 
               if (m.getName().equals("deserialize")) 
                  newtypes = m.getGenericParameterTypes();
         }
         else 
            for (Method m: ((Class) ((ParameterizedType) c).getRawType()).getDeclaredMethods()) 
               if (m.getName().equals("deserialize")) 
                  newtypes = m.getGenericParameterTypes();

         if (null == newtypes) throw new DBusException("Serializable classes must implement a deserialize method");

         String[] sigs = new String[newtypes.length];
         for (int j = 0; j < sigs.length; j++) {
            String[] ss = recursiveGetDBusType(newtypes[j], false, level+1);
            if (1 != ss.length) throw new DBusException("Serializable classes must serialize to native DBus types");
            sigs[j] = ss[0];
         }
         return sigs;
      } else {
         throw new DBusException("Exporting non-exportable type "+c);
      }

      return new String[] { out[level].toString() };
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
   public static String getJavaType(String dbus, Set<String> imports, Map<StructStruct,String> structs, boolean container, boolean fullnames) throws DBusException
   {
      if (null == dbus || "".equals(dbus)) return "";
      
      try {
         switch(dbus.charAt(0)) {
            case '(':
               String name = "Struct";
               if (null != structs) {
                  int num = 1;
                  while (null != structs.get(new StructStruct(name+num))) num++;
                  name = name+num;
                  structs.put(new StructStruct(name), dbus.substring(1, dbus.length()-1));
               }
               if (fullnames) return "org.freedesktop.dbus.Struct";
               else return name;
            case 'a':
               if ('{' == dbus.charAt(1)) {
                  if (null != imports) imports.add("java.util.Map");
                  if (fullnames) return "java.util.Map";
                  else return "Map<"+getJavaType(dbus.substring(2,3), imports, structs, true, false)+", "+
                     getJavaType(dbus.substring(3,dbus.length()-1), imports, structs, true, false)+">";
               }
               if (null != imports) imports.add("java.util.List");
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
         if (DBusConnection.EXCEPTION_DEBUG) IOOBe.printStackTrace();
         throw new DBusException("Failed to parse DBus type signature: "+dbus);
      }
   }
   /**
    * Recursively converts types for serialization onto DBus.
    * @param parameters The parameters to convert.
    * @param types The (possibly generic) types of the parameters.
    * @return The converted parameters.
    * @throws DBusException Thrown if there is an error in converting the objects.
    */
   @SuppressWarnings("unchecked")
   public static Object[] convertParameters(Object[] parameters, Type[] types) throws DBusException
   {
      if (null == parameters) return null;
      for (int i = 0; i < parameters.length; i++) {
         if (null == parameters[i]) continue;

         if (types[i] instanceof Class &&
               DBusSerializable.class.isAssignableFrom((Class) types[i])) {
            for (Method m: ((Class) types[i]).getDeclaredMethods()) 
               if (m.getName().equals("deserialize")) {
                  Type[] newtypes = m.getGenericParameterTypes();
                  Type[] expand = new Type[types.length + newtypes.length - 1];
                  System.arraycopy(types, 0, expand, 0, i); 
                  System.arraycopy(newtypes, 0, expand, i, newtypes.length); 
                  System.arraycopy(types, i+1, expand, i+newtypes.length, types.length-i-1); 
                  types = expand;
               }
         } else 
            parameters[i] = convertParameter(parameters[i], types[i]);
        
      }
      return parameters;
   }
   @SuppressWarnings("unchecked")
   static Object convertParameter(Object parameter, Type type) throws DBusException
   {
      // its an unwrapped variant, wrap it
      if (type instanceof TypeVariable &&
            !(parameter instanceof Variant)) {
         parameter = new Variant<Object>(parameter);
      }

      // its something parameterised
      else if (type instanceof ParameterizedType) {
         ParameterizedType p = (ParameterizedType) type;
         Class r = (Class) p.getRawType();

         // its a list, wrap it in our typed container class
         if (List.class.isAssignableFrom(r)) {
            parameter = new ListContainer((List<Object>) parameter, p);
         }
         // its a map, wrap it in our typed container class
         else if (Map.class.isAssignableFrom(r)) {
            parameter = new MapContainer((Map<Object,Object>) parameter, p);
         }
         // its a struct, recurse over it
         else if (Struct.class.isAssignableFrom(r)) {
            Constructor con = r.getDeclaredConstructors()[0];
            Field[] fs = r.getDeclaredFields();
            Type[] ts = new Type[fs.length];
            for (Field f : fs) {
               Position pos = f.getAnnotation(Position.class);
               if (null == pos) continue;
               ts[pos.value()] = f.getGenericType();
            }
            Object[] oldparams = ((Struct) parameter).getParameters();
            Object[] newparams = convertParameters(oldparams, ts);
            try {
               parameter = con.newInstance(newparams);
            } catch (Exception e) {
               if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
               throw new DBusException("Failure in serializing parameters: "+e.getMessage());
            }
         }
      }

      else if (type instanceof GenericArrayType) {
         if (Array.getLength(parameter) > MAX_ARRAY_LENGTH) throw new DBusException("Array exceeds maximum length of "+MAX_ARRAY_LENGTH);
         Type t = ((GenericArrayType) type).getGenericComponentType();
         if (!(t instanceof Class) || !((Class) t).isPrimitive())
            parameter = new ListContainer((Object[]) parameter, t);
      } else if (type instanceof Class && ((Class) type).isArray()) {
         if (Array.getLength(parameter) > MAX_ARRAY_LENGTH) throw new DBusException("Array exceeds maximum length of "+MAX_ARRAY_LENGTH);
         if (!((Class) type).getComponentType().isPrimitive())
            parameter = new ListContainer((Object[]) parameter, ((Class) type).getComponentType());
      }
      return parameter;
   }
   static Object deSerializeParameter(Object parameter, Type type) throws Exception
   {
      if (null == parameter) 
         return null;

      // its a wrapped variant, unwrap it
      if (type instanceof TypeVariable 
            && parameter instanceof Variant) {
         parameter = ((Variant)parameter).getValue();
      }

      // its a wrapped map, unwrap it
      if (parameter instanceof MapContainer)
         parameter = ((MapContainer) parameter).getMap(type);

      // its a wrapped list, unwrap it
      if (parameter instanceof ListContainer) {
         parameter = ((ListContainer) parameter).getList(type);
      }

      // its an object path, get/create the proxy
      if (parameter instanceof ObjectPath) {
         parameter = ((ObjectPath) parameter).conn.getExportedObject(
               ((ObjectPath) parameter).source,
               ((ObjectPath) parameter).path);
      }
      
      // it should be a struct. create it
      if (parameter instanceof Object[] && 
            type instanceof Class &&
            Struct.class.isAssignableFrom((Class) type)) {
         Type[] ts = Struct.getStructTypeCache(type);
         if (null == ts) {
            Field[] fs = ((Class) type).getDeclaredFields();
            ts = new Type[fs.length];
            for (Field f : fs) {
               Position p = f.getAnnotation(Position.class);
               if (null == p) continue;
               ts[p.value()] = f.getGenericType();
           }
            Struct.putStructTypeCache(type, ts);
         }

         // recurse over struct contents
         parameter = deSerializeParameters((Object[]) parameter, ts);
         for (Constructor con: ((Class) type).getDeclaredConstructors()) {
            try {
               parameter = con.newInstance((Object[]) parameter);
               break;
            } catch (IllegalArgumentException IAe) {}
         }
      }

      // recurse over arrays
      if (parameter instanceof Object[]) {
         Type[] ts = new Type[((Object[]) parameter).length];
         Arrays.fill(ts, parameter.getClass().getComponentType());
         parameter = deSerializeParameters((Object[]) parameter,
               ts);
      }

      // make sure arrays are in the correct format
      if (parameter instanceof Object[] ||
            parameter instanceof List ||
            parameter.getClass().isArray()) {
         if (type instanceof ParameterizedType)
            parameter = ArrayFrob.convert(parameter,
                  (Class<? extends Object>) ((ParameterizedType) type).getRawType());
         else if (type instanceof GenericArrayType) {
            Type ct = ((GenericArrayType) type).getGenericComponentType();
            Class cc = null;
            if (ct instanceof Class)
               cc = (Class) ct;
            if (ct instanceof ParameterizedType)
               cc = (Class) ((ParameterizedType) ct).getRawType();
            Object o = Array.newInstance(cc, 0);
            parameter = ArrayFrob.convert(parameter,
                  o.getClass());
         } else if (type instanceof Class &&
               ((Class) type).isArray()) {
            Class cc = ((Class) type).getComponentType();
            Object o = Array.newInstance(cc, 0);
            parameter = ArrayFrob.convert(parameter,
                  o.getClass());
         }
      }
      return parameter;
   }
   static Object[] deSerializeParameters(Object[] parameters, Type[] types) throws Exception
   {
      if (null == parameters) return null;
      for (int i = 0; i < parameters.length; i++) {
      if (null == parameters[i]) continue;

      if (types[i] instanceof Class &&
            DBusSerializable.class.isAssignableFrom((Class) types[i])) {
         for (Method m: ((Class) types[i]).getDeclaredMethods()) 
            if (m.getName().equals("deserialize")) {
               Type[] newtypes = m.getGenericParameterTypes();
               try {
                  Object[] sub = new Object[newtypes.length];
                  System.arraycopy(parameters, i, sub, 0, newtypes.length); 
                  sub = deSerializeParameters(sub, newtypes);
                  DBusSerializable sz = (DBusSerializable) ((Class) types[i]).newInstance();
                  m.invoke(sz, sub);
                  Object[] compress = new Object[parameters.length - newtypes.length + 1];
                  System.arraycopy(parameters, 0, compress, 0, i);
                  compress[i] = sz;
                  System.arraycopy(parameters, i + newtypes.length, compress, i+1, parameters.length - i - newtypes.length);
                  parameters = compress;
               } catch (ArrayIndexOutOfBoundsException AIOOBe) {
                  if (DBusConnection.EXCEPTION_DEBUG) AIOOBe.printStackTrace();
                  throw new DBusException("Not enough elements to create custom object from serialized data ("+(parameters.length-i)+" < "+(newtypes.length)+")");
               }
            }
      } else
         parameters[i] = deSerializeParameter(parameters[i], types[i]);
      }
      return parameters;
   }

   private DBusConnection() throws DBusException
   {
      exportedObjects = new HashMap<String,ExportedObject>();
      importedObjects = new HashMap<DBusInterface,RemoteObject>();
      exportedObjects.put(null, new ExportedObject(new _globalhandler()));
      handledSignals = new HashMap<SignalTuple,Vector<DBusSigHandler>>();
      pendingCalls = new EfficientMap(PENDING_MAP_INITIAL_SIZE);
      busnames = new Vector<String>();
      outgoing = new EfficientQueue(PENDING_MAP_INITIAL_SIZE);
      pendingErrors = new LinkedList<DBusErrorMessage>();
      runnables = new LinkedList<Runnable>();
      workers = new LinkedList<_workerthread>();
      objectTree = new ObjectTree();
      synchronized (workers) {
         for (int i = 0; i < THREADCOUNT; i++) {
            _workerthread t = new _workerthread();
            t.start();
            workers.add(t);
         }
      }
      _run = true;
      synchronized (_reflock) {
         _refcount = 1; 
      }
   }
   @SuppressWarnings("unchecked")
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
         busnames.add(_dbus.Hello());
      } catch (DBusExecutionException DBEe) {
         if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
         throw new DBusException(DBEe.getMessage());
      }
      // register disconnect handlers
      addSigHandler(org.freedesktop.DBus.Local.Disconnected.class, new _sighandler());
   }


   @SuppressWarnings("unchecked")
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
         busnames.add(_dbus.Hello());
      } catch (DBusExecutionException DBEe) {
         if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
         throw new DBusException(DBEe.getMessage());
      }
      // register disconnect handlers
      addSigHandler(org.freedesktop.DBus.Local.Disconnected.class, new _sighandler());
   }

   /**
    * Change the number of worker threads to receive method calls and handle signals.
    * Default is 4 threads
    * @param newcount The new number of worker Threads to use.
    */
   public void changeThreadCount(byte newcount)
   {
      synchronized (workers) {
         if (workers.size() > newcount) {
            int n = workers.size() - newcount;
            for (int i = 0; i < n; i++) {
               _workerthread t = workers.removeFirst();
               t.halt();
            }
         } else if (workers.size() < newcount) {
            int n = newcount-workers.size();
            for (int i = 0; i < n; i++) {
               _workerthread t = new _workerthread();
               t.start();
               workers.add(t);
            }
         }
      }
   }
   private void addRunnable(Runnable r)
   {
      synchronized(runnables) {
         runnables.add(r);
         runnables.notifyAll();
      }
   }

   String getExportedObject(DBusInterface i) throws DBusException
   {
      for (String s: exportedObjects.keySet())
         if (exportedObjects.get(s).object.equals(i))
            return s;

      String s = importedObjects.get(i).objectpath;
      if (null != s) return s;

      throw new DBusException("Not an object exported or imported by this connection"); 
   }

   DBusInterface dynamicProxy(String source, String path) throws DBusException
   {
      try {
         DBus.Introspectable intro = (DBus.Introspectable) getRemoteObject(source, path, DBus.Introspectable.class);
         String data = intro.Introspect();
         String[] tags = data.split("[<>]");
         Vector<String> ifaces = new Vector<String>();
         for (String tag: tags) {
            if (tag.startsWith("interface")) {
               ifaces.add(tag.replaceAll("^interface *name *= *['\"]([^'\"]*)['\"].*$", "$1"));
            }
         }
         Vector<Class> ifcs = new Vector<Class>();
         for(String iface: ifaces) {
            int j = 0;
            while (j >= 0) {
               try {
                  ifcs.add(Class.forName(iface));
                  break;
               } catch (Exception e) {}
               j = iface.lastIndexOf(".");
               char[] cs = iface.toCharArray();
               if (j >= 0) {
                  cs[j] = '$';
                  iface = String.valueOf(cs);
               }
            }
         }

         if (ifcs.size() == 0) throw new DBusException("Could not find an interface to cast to");

         RemoteObject ro = new RemoteObject(source, path, null, false);
         DBusInterface newi =  (DBusInterface) Proxy.newProxyInstance(ifcs.get(0).getClassLoader(), 
               (Class[]) ifcs.toArray(new Class[0]), new RemoteInvocationHandler(this, ro));
         importedObjects.put(newi, ro);
         return newi;
      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusException("Failed to create proxy object for "+path+" exported by "+source+". Reason: "+e.getMessage());
      }
   }
   
   DBusInterface getExportedObject(String source, String path) throws DBusException
   {
      ExportedObject o = exportedObjects.get(path);
      if (null != o) return o.object;
      if (null == source) throw new DBusException("Not an object exported by this connection and no remote specified");
      return dynamicProxy(source, path);
   }

   /**
    * Returns a structure with information on the current method call.
    * @return the DBusCallInfo for this method call, or null if we are not in a method call.
    */
   public static DBusCallInfo getCallInfo() 
   {
      DBusCallInfo info;
      synchronized (infomap) {
         info = infomap.get(Thread.currentThread());
      }
      return info;
   }

   /** 
    * Register a bus name.
    * Register the well known name that this should respond to on the Bus.
    * This function is deprecated in favour of requestBusName.
    * @param busname The name to respond to. MUST be in dot-notation like "org.freedesktop.local"
    * @throws DBusException If the register name failed, or our name already exists on the bus.
    *  or if busname is incorrectly formatted.
    * @see #requestBusName
    * @deprecated
    */
   @Deprecated()
   public void registerService(String busname) throws DBusException
   {
      requestBusName(busname);
   }
   /** 
    * Request a bus name.
    * Request the well known name that this should respond to on the Bus.
    * @param busname The name to respond to. MUST be in dot-notation like "org.freedesktop.local"
    * @throws DBusException If the register name failed, or our name already exists on the bus.
    *  or if busname is incorrectly formatted.
    */
   public void requestBusName(String busname) throws DBusException
   {
      if (!busname.matches(BUSNAME_REGEX)||busname.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid bus name");
      synchronized (this.busnames) {
         UInt32 rv;
         try { 
            rv = _dbus.RequestName(busname, 
                  new UInt32(DBus.DBUS_NAME_FLAG_REPLACE_EXISTING |
                     DBus.DBUS_NAME_FLAG_DO_NOT_QUEUE));
         } catch (DBusExecutionException DBEe) {
            if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
            throw new DBusException(DBEe.getMessage());
         }
         switch (rv.intValue()) {
            case DBus.DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER: break;
            case DBus.DBUS_REQUEST_NAME_REPLY_IN_QUEUE: throw new DBusException("Failed to register bus name");
            case DBus.DBUS_REQUEST_NAME_REPLY_EXISTS: throw new DBusException("Failed to register bus name");
            case DBus.DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER: break;
            default: break;
         }
         this.busnames.add(busname);
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
      if (!objectpath.matches(OBJECT_REGEX)||objectpath.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid object path ("+objectpath+")");
      if (null == objectpath || "".equals(objectpath)) 
         throw new DBusException("Must Specify an Object Path");
      synchronized (exportedObjects) {
         if (null != exportedObjects.get(objectpath)) 
            throw new DBusException("Object already exported");
         ExportedObject eo = new ExportedObject(object);
         exportedObjects.put(objectpath, eo);
         objectTree.add(objectpath, object, eo.introspectiondata);
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
       * Return a reference to a remote object. 
       * This method will resolve the well known name (if given) to a unique bus name when you call it.
       * This means that if a well known name is released by one process and acquired by another calls to
       * objects gained from this method will continue to operate on the original process.
       * @param busname The bus name to connect to. Usually a well known bus name in dot-notation (such as "org.freedesktop.local")
       * or may be a DBus address such as ":1-16".
       * @param objectpath The path on which the process is exporting the object.$
       * @param type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
       * as the interface the remote object is exporting.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
   */
   public DBusInterface getPeerRemoteObject(String busname, String objectpath, Class<? extends DBusInterface> type) throws DBusException
   {
      return getPeerRemoteObject(busname, objectpath, type, true);
   }
   /** 
       * Return a reference to a remote object. 
       * This method will resolve the well known name (if given) to a unique bus name when you call it.
       * This means that if a well known name is released by one process and acquired by another calls to
       * objects gained from this method will continue to operate on the original process.
       * 
       * This method will use bus introspection to determine the interfaces on a remote object and so
       * <b>may block</b> and <b>may fail</b>. The resulting proxy object will, however, be castable
       * to any interface it implements. It will also autostart the process if applicable. Also note
       * that the resulting proxy may fail to execute the correct method with overloaded methods
       * and that complex types may fail in interesting ways. Basically, if something odd happens, 
       * try specifying the interface explicitly.
       * 
       * @param busname The bus name to connect to. Usually a well known bus name in dot-notation (such as "org.freedesktop.local")
       * or may be a DBus address such as ":1-16".
       * @param objectpath The path on which the process is exporting the object.$
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
   */
   public DBusInterface getPeerRemoteObject(String busname, String objectpath) throws DBusException
   {
      if (null == busname) throw new DBusException("Invalid bus name (null)");
      
      if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX))
            || busname.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid bus name ("+busname+")");
      
      String unique = _dbus.GetNameOwner(busname);

      return dynamicProxy(busname, objectpath);
   }

   /** 
       * Return a reference to a remote object. 
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       * 
       * This method will use bus introspection to determine the interfaces on a remote object and so
       * <b>may block</b> and <b>may fail</b>. The resulting proxy object will, however, be castable
       * to any interface it implements. It will also autostart the process if applicable. Also note
       * that the resulting proxy may fail to execute the correct method with overloaded methods
       * and that complex types may fail in interesting ways. Basically, if something odd happens, 
       * try specifying the interface explicitly.
       * 
       * @param busname The bus name to connect to. Usually a well known bus name name in dot-notation (such as "org.freedesktop.local")
       * or may be a DBus address such as ":1-16".
       * @param objectpath The path on which the process is exporting the object.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
    */
   public DBusInterface getRemoteObject(String busname, String objectpath) throws DBusException
   {
      if (null == busname) throw new DBusException("Invalid bus name (null)");
      if (null == objectpath) throw new DBusException("Invalid object path (null)");
      
      if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX))
         || busname.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid bus name ("+busname+")");
      
      if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid object path ("+objectpath+")");
      
      return dynamicProxy(busname, objectpath);
   }

   /** 
       * Return a reference to a remote object. 
       * This method will resolve the well known name (if given) to a unique bus name when you call it.
       * This means that if a well known name is released by one process and acquired by another calls to
       * objects gained from this method will continue to operate on the original process.
       * @param busname The bus name to connect to. Usually a well known bus name in dot-notation (such as "org.freedesktop.local")
       * or may be a DBus address such as ":1-16".
       * @param objectpath The path on which the process is exporting the object.$
       * @param type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
       * as the interface the remote object is exporting.
       * @param autostart Disable/Enable auto-starting of services in response to calls on this object. 
       * Default is enabled; when calling a method with auto-start enabled, if the destination is a well-known name
       * and is not owned the bus will attempt to start a process to take the name. When disabled an error is
       * returned immediately.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
    */
   public DBusInterface getPeerRemoteObject(String busname, String objectpath, Class<? extends DBusInterface> type, boolean autostart) throws DBusException
   {
      if (null == busname) throw new DBusException("Invalid bus name (null)");
      
      if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX))
            || busname.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid bus name ("+busname+")");
      
      String unique = _dbus.GetNameOwner(busname);

      return getRemoteObject(unique, objectpath, type, autostart);
   }
   /** 
       * Return a reference to a remote object. 
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       * @param busname The bus name to connect to. Usually a well known bus name name in dot-notation (such as "org.freedesktop.local")
       * or may be a DBus address such as ":1-16".
       * @param objectpath The path on which the process is exporting the object.
       * @param type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
       * as the interface the remote object is exporting.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
    */
   public DBusInterface getRemoteObject(String busname, String objectpath, Class<? extends DBusInterface> type) throws DBusException
   {
      return getRemoteObject(busname, objectpath, type, true);
   }
   /** 
       * Return a reference to a remote object. 
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       * @param busname The bus name to connect to. Usually a well known bus name name in dot-notation (such as "org.freedesktop.local")
       * or may be a DBus address such as ":1-16".
       * @param objectpath The path on which the process is exporting the object.
       * @param type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
       * as the interface the remote object is exporting.
       * @param autostart Disable/Enable auto-starting of services in response to calls on this object. 
       * Default is enabled; when calling a method with auto-start enabled, if the destination is a well-known name
       * and is not owned the bus will attempt to start a process to take the name. When disabled an error is
       * returned immediately.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
    */
   public DBusInterface getRemoteObject(String busname, String objectpath, Class<? extends DBusInterface> type, boolean autostart) throws DBusException
   {
      if (null == busname) throw new DBusException("Invalid bus name (null)");
      if (null == objectpath) throw new DBusException("Invalid object path (null)");
      if (null == type) throw new ClassCastException("Not A DBus Interface");
      
      if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX))
         || busname.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid bus name ("+busname+")");
      
      if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid object path ("+objectpath+")");
      
      if (!DBusInterface.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Interface");
      
      RemoteObject ro = new RemoteObject(busname, objectpath, type, autostart);
      DBusInterface i =  (DBusInterface) Proxy.newProxyInstance(type.getClassLoader(), 
            new Class[] { type }, new RemoteInvocationHandler(this, ro));
      importedObjects.put(i, ro);
      return i;
   }
   /** 
    * Send a signal.
    * @param signal The signal to send.
    */
   public void sendSignal(DBusSignal signal)
   {
      synchronized (outgoing) {
         outgoing.add(signal); }
   }
   /** 
    * Remove a Signal Handler.
    * Stops listening for this signal.
    * @param type The signal to watch for. 
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      removeSigHandler(new DBusMatchRule(type), handler);
   }
   /** 
    * Remove a Signal Handler.
    * Stops listening for this signal.
    * @param type The signal to watch for. 
    * @param object The object emitting the signal.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusInterface object,  DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      String objectpath = importedObjects.get(object).objectpath;
      if (!objectpath.matches(OBJECT_REGEX)||objectpath.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid object path ("+objectpath+")");
      removeSigHandler(new DBusMatchRule(type, null, objectpath), handler);
   }
   /** 
    * Remove a Signal Handler.
    * Stops listening for this signal.
    * @param type The signal to watch for. 
    * @param source The source of the signal.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void removeSigHandler(Class<T> type, String source, DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      if (source.matches(BUSNAME_REGEX)) throw new DBusException("Cannot watch for signals based on well known bus name as source, only unique names.");
      if (!source.matches(CONNID_REGEX)||source.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid bus name ("+source+")");
      removeSigHandler(new DBusMatchRule(type, source, null), handler);
   }
   /** 
    * Remove a Signal Handler.
    * Stops listening for this signal.
    * @param type The signal to watch for. 
    * @param source The source of the signal.
    * @param object The object emitting the signal.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void removeSigHandler(Class<T> type, String source, DBusInterface object,  DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      if (source.matches(BUSNAME_REGEX)) throw new DBusException("Cannot watch for signals based on well known bus name as source, only unique names.");
      if (!source.matches(CONNID_REGEX)||source.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid bus name ("+source+")");
      String objectpath = importedObjects.get(object).objectpath;
      if (!objectpath.matches(OBJECT_REGEX)||objectpath.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid object path ("+objectpath+")");
      removeSigHandler(new DBusMatchRule(type, source, objectpath), handler);
   }
   private <T extends DBusSignal> void removeSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException
   {
      
      SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
      synchronized (handledSignals) {
         Vector<DBusSigHandler> v = handledSignals.get(key);
         if (null != v) {
            v.remove(handler);
            if (0 == v.size()) {
               handledSignals.remove(key);
               try {
                  _dbus.RemoveMatch(rule.toString());
               } catch (DBusExecutionException DBEe) {
                  if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
                  throw new DBusException(DBEe.getMessage());
               }
            }
         } 
      }
   }
   /** 
    * Add a Signal Handler.
    * Adds a signal handler to call when a signal is received which matches the specified type and name.
    * @param type The signal to watch for. 
    * @param handler The handler to call when a signal is received.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      addSigHandler(new DBusMatchRule(type), handler);
   }
   /** 
    * Add a Signal Handler.
    * Adds a signal handler to call when a signal is received which matches the specified type, name and object.
    * @param type The signal to watch for. 
    * @param object The object from which the signal will be emitted
    * @param handler The handler to call when a signal is received.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusInterface object, DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      String objectpath = importedObjects.get(object).objectpath;
      if (!objectpath.matches(OBJECT_REGEX)||objectpath.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid object path ("+objectpath+")");
      addSigHandler(new DBusMatchRule(type, null, objectpath), handler);
   }
   /** 
    * Add a Signal Handler.
    * Adds a signal handler to call when a signal is received which matches the specified type, name and source.
    * @param type The signal to watch for. 
    * @param source The process which will send the signal. This <b>MUST</b> be a unique bus name and not a well known name.
    * @param handler The handler to call when a signal is received.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void addSigHandler(Class<T> type, String source, DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      if (source.matches(BUSNAME_REGEX)) throw new DBusException("Cannot watch for signals based on well known bus name as source, only unique names.");
      if (!source.matches(CONNID_REGEX)||source.length() > MAX_NAME_LENGTH) 
         throw new DBusException("Invalid bus name ("+source+")");
      addSigHandler(new DBusMatchRule(type, source, null), handler);
   }
   /** 
    * Add a Signal Handler.
    * Adds a signal handler to call when a signal is received which matches the specified type, name, source and object.
    * @param type The signal to watch for. 
    * @param source The process which will send the signal. This <b>MUST</b> be a unique bus name and not a well known name.
    * @param object The object from which the signal will be emitted
    * @param handler The handler to call when a signal is received.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
   public <T extends DBusSignal> void addSigHandler(Class<T> type, String source, DBusInterface object,  DBusSigHandler<T> handler) throws DBusException
   {
      if (!DBusSignal.class.isAssignableFrom(type)) throw new ClassCastException("Not A DBus Signal");
      if (source.matches(BUSNAME_REGEX)) throw new DBusException("Cannot watch for signals based on well known bus name as source, only unique names.");
      if (!source.matches(CONNID_REGEX)||source.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid bus name ("+source+")");
      String objectpath = importedObjects.get(object).objectpath;
      if (!objectpath.matches(OBJECT_REGEX)||objectpath.length() > MAX_NAME_LENGTH)
         throw new DBusException("Invalid object path ("+objectpath+")");
      addSigHandler(new DBusMatchRule(type, source, objectpath), handler);
   }
   private <T extends DBusSignal> void addSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException
   {
      try {
         _dbus.AddMatch(rule.toString());
      } catch (DBusExecutionException DBEe) {
         if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
         throw new DBusException(DBEe.getMessage());
      }
      SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
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
               while (runnables.size() > 0)
                  synchronized (runnables) {
                     runnables.notifyAll();
                  }
               _run = false;
               try {
                  synchronized (thread) { thread.wait(); }
               } catch (InterruptedException Ie) {}
               dbus_disconnect(connid);
               conn.remove(connkey);
               synchronized(workers) {
                  for (_workerthread t: workers)
                     t.halt();
               }
               synchronized (runnables) {
                  runnables.notifyAll();
               }
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

   /**
    * Call a method asynchronously and get a handle with which to get the reply.
    * @param object The remote object on which to call the method.
    * @param m The name of the method on the interface to call.
    * @param parameters The parameters to call the method with.
    * @return A handle to the call.
    */
   public DBusAsyncReply callMethodAsync(DBusInterface object, String m, Object... parameters)
   {
      Class[] types = new Class[parameters.length];
      for (int i = 0; i < parameters.length; i++) 
         types[i] = parameters[i].getClass();
      RemoteObject ro = importedObjects.get(object);

      try {
         Method me;
         if (null == ro.iface)
            me = object.getClass().getMethod(m, types);
         else
            me = ro.iface.getMethod(m, types);
         return (DBusAsyncReply) RemoteInvocationHandler.executeRemoteMethod(ro, me, this, true, parameters);
      } catch (DBusExecutionException DBEe) {
         if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
         throw DBEe;
      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         throw new DBusExecutionException(e.getMessage());
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
         meth = eo.methods.get(new MethodTuple(m.getName(), m.getSig()));
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
               outgoing.add(new DBusErrorMessage(m, new DBus.Error.UnknownObject(m.getObjectPath()+" is not an object provided by this process."))); }
            return;
         }
         meth = eo.methods.get(new MethodTuple(m.getName(), m.getSig()));
         if (null == meth) {
            synchronized (outgoing) {
               outgoing.add(new DBusErrorMessage(m, new DBus.Error.UnknownMethod("The method `"+m.getType()+"."+m.getName()+"' does not exist on this object."))); }
            return;
         }
         o = eo.object;
      }

      // now execute it
      final Method me = meth;
      final Object ob = o;
      final EfficientQueue outqueue = outgoing;
      final boolean noreply = (1 == (m.getFlags() & MethodCall.NO_REPLY));
      final DBusCallInfo info = new DBusCallInfo(m);
      addRunnable(new Runnable() 
      { 
         public void run() 
         { 
            try {
               Type[] ts = me.getGenericParameterTypes();
               m.parameters = deSerializeParameters(m.parameters, ts);
            } catch (Exception e) {
               if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
               synchronized (outqueue) {
                  outqueue.add(new DBusErrorMessage(m, new DBus.Error.UnknownMethod("Failure in de-serializing message ("+e+")"))); }
               return;
            }

            try { 
               synchronized (infomap) {
                  infomap.put(Thread.currentThread(), info);
               }
               Object result;
               try {
               result = me.invoke(ob, m.parameters);
               } catch (InvocationTargetException ITe) {
                  if (DBusConnection.EXCEPTION_DEBUG) ITe.getCause().printStackTrace();
                  throw ITe.getCause();
               }
               synchronized (infomap) {
                  infomap.remove(Thread.currentThread());
               }
               if (!noreply) {
                  MethodReply reply;
                  if (Void.TYPE.equals(me.getReturnType())) 
                     reply = new MethodReply(m);
                  else {
                     result = convertParameter(result, me.getGenericReturnType());
                     reply = new MethodReply(m, result);
                  }
                  synchronized (outqueue) {
                     outqueue.add(reply);
                  }
               }
            } catch (DBusExecutionException DBEe) {
               if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
               synchronized (outqueue) {
                  outqueue.add(new DBusErrorMessage(m, DBEe)); 
               }
            } catch (Throwable e) {
               if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
               synchronized (outqueue) {
                  outqueue.add(new DBusErrorMessage(m, new DBusExecutionException("Error Executing Method "+m.getType()+"."+m.getName()+": "+e.getMessage()))); 
               }
            } 
         }
      });
   }
   @SuppressWarnings("unchecked")
   private void handleMessage(final DBusSignal s)
   {
      Vector<DBusSigHandler> v = new Vector<DBusSigHandler>();
      synchronized(handledSignals) {
         Vector<DBusSigHandler> t;
         t = handledSignals.get(new SignalTuple(s.getType(), s.getName(), null, null));
         if (null != t) v.addAll(t);
         t = handledSignals.get(new SignalTuple(s.getType(), s.getName(), s.getObjectPath(), null));
         if (null != t) v.addAll(t);
         t = handledSignals.get(new SignalTuple(s.getType(), s.getName(), null, s.getSource()));
         if (null != t) v.addAll(t);
         t = handledSignals.get(new SignalTuple(s.getType(), s.getName(), s.getObjectPath(), s.getSource()));
         if (null != t) v.addAll(t);
      }
      if (0 == v.size()) return;
      for (final DBusSigHandler h: v)
         addRunnable(new Runnable() { public void run() {
            {
               h.handle(s); 
            }
         } });
   }
   private void handleMessage(final DBusErrorMessage err)
   {
      MethodCall m = null;
      synchronized (pendingCalls) {
         if (pendingCalls.contains(err.getReplySerial()))
            m = pendingCalls.remove(err.getReplySerial());
      }
      if (null != m)
         m.setReply(err);
      else
         synchronized (pendingErrors) {
            pendingErrors.addLast(err); }
   }
   private void handleMessage(final MethodReply mr)
   {
      MethodCall m = null;
      synchronized (pendingCalls) {
         if (pendingCalls.contains(mr.getReplySerial()))
            m = pendingCalls.remove(mr.getReplySerial());
      }
      if (null != m) {
         m.setReply(mr);
         mr.setCall(m);
      } else
         synchronized (outgoing) {
            outgoing.add(new DBusErrorMessage(mr, new DBusExecutionException("Spurious reply. No message with the given serial id was awaiting a reply."))); 
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
               int flags = ((MethodCall) m).getFlags();
               m.setSerial(dbus_call_method(connid, ((MethodCall) m).getDestination(), ((MethodCall) m).getObjectPath(), m.getType(), m.getName(), flags, m.getParameters()));
               if (0 < m.getSerial()) {
                  if (0 == (flags & MethodCall.NO_REPLY))
                     pendingCalls.put(m.getSerial(),(MethodCall) m);
               }
               else
                  ((MethodCall) m).setReply(new DBusErrorMessage(m, new InternalMessageException("Message Failed to Send")));
            }
         } catch (DBusExecutionException DBEe) {
            if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
            ((MethodCall) m).setReply(new DBusErrorMessage(m, DBEe));
         } catch (Exception e) {
            if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
            ((MethodCall) m).setReply(new DBusErrorMessage(m, new DBusExecutionException("Message Failed to Send: "+e.getMessage())));
         }
      }
      else if (m instanceof MethodReply) {
         try {
            m.setSerial(dbus_reply_to_call(connid, ((MethodReply) m).getDestination(), m.getType(), ((MethodReply) m).getObjectPath(), m.getName(), m.getReplySerial(), m.getParameters()));
         } catch (Exception e) {
            if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
            dbus_send_error_message(connid, ((MethodReply) m).getDestination(), DBusExecutionException.class.getName(), m.getReplySerial(), new Object[] { "Error sending reply: "+e.getMessage() });
         }
      }
   }
   private DBusMessage readIncoming(int timeoutms, EfficientQueue outgoing)
   {
      DBusMessage m = dbus_read_write_pop(connid, timeoutms, outgoing);
      return m;
   }
}
