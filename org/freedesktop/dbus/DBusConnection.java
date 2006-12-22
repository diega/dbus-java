/*
   D-Bus Java Bindings
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
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

import java.io.IOException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import java.util.regex.Pattern;

import org.freedesktop.DBus;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

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
            try {
               Error err = new Error(
                     busnames.get(0), "org.freedesktop.DBus.Local.Disconnected", 0, "s", new Object[] { "Disconnected" });
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
            } catch (DBusException DBe) {}
         } else if (s instanceof org.freedesktop.DBus.NameAcquired) {
            busnames.add(((org.freedesktop.DBus.NameAcquired) s).name);
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
            Message m = null;
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
                     else if (m instanceof MethodReturn)
                        handleMessage((MethodReturn) m);
                     else if (m instanceof Error)
                        handleMessage((Error) m);

                     m = null;
                  }
               } catch (IOException IOe) { 
                  try {
                     handleMessage(new org.freedesktop.DBus.Local.Disconnected("/"));
                  } catch (Exception e) {}
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
         String intro = objectTree.Introspect(objectpath);
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
   public static final String DEFAULT_SYSTEM_BUS_ADDRESS = "unix:path=/var/run/dbus/system_bus_socket";

   private Map<String,ExportedObject> exportedObjects;
   private ObjectTree objectTree;
   private Map<DBusInterface,RemoteObject> importedObjects;
   private Map<SignalTuple,Vector<DBusSigHandler>> handledSignals;
   private EfficientMap pendingCalls;
   private List<String> busnames;
   private LinkedList<Runnable> runnables;
   private LinkedList<_workerthread> workers;
   private boolean _run;
   private int connid;
   EfficientQueue outgoing;
   LinkedList<Error> pendingErrors;

   private static final Map<Object,DBusConnection> conn = new HashMap<Object,DBusConnection>();
   private static final Map<Thread,DBusCallInfo> infomap = new HashMap<Thread,DBusCallInfo>();
   private int _refcount = 0;
   private Object _reflock = new Object();
   private Object connkey;
   private DBus _dbus;
   private _thread thread;
   private Transport transport;
   private String addr;
   static final Pattern dollar_pattern = Pattern.compile("[$]");
   public static final boolean EXCEPTION_DEBUG;
   public static final boolean DBUS_JAVA_DEBUG;
   static {
      EXCEPTION_DEBUG = (null == System.getenv("DBUS_JAVA_EXCEPTION_DEBUG"));
      DBUS_JAVA_DEBUG = (null == System.getenv("DBUS_JAVA_DEBUG"));
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
         String s = null;
         switch (bustype) {
            case SYSTEM:
               s = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
               if (null == s) s = DEFAULT_SYSTEM_BUS_ADDRESS;
               break;
            case SESSION:
               s = System.getenv("DBUS_SESSION_BUS_ADDRESS");
               if (null == s) throw new DBusException("Cannot Resolve Session Bus Address");
               break;
            default:
               throw new DBusException("Invalid Bus Type: "+bustype);
         }
         DBusConnection c = conn.get(s);
         if (null != c) {
            synchronized (c._reflock) { c._refcount++; }
            return c;
         }
         else {
            c = new DBusConnection(s);
            conn.put(s, c);
            return c;
         }
      }
   }
   @SuppressWarnings("unchecked")
   private DBusConnection(String address) throws DBusException
   {
      exportedObjects = new HashMap<String,ExportedObject>();
      importedObjects = new HashMap<DBusInterface,RemoteObject>();
      exportedObjects.put(null, new ExportedObject(new _globalhandler()));
      handledSignals = new HashMap<SignalTuple,Vector<DBusSigHandler>>();
      pendingCalls = new EfficientMap(PENDING_MAP_INITIAL_SIZE);
      busnames = new Vector<String>();
      outgoing = new EfficientQueue(PENDING_MAP_INITIAL_SIZE);
      pendingErrors = new LinkedList<Error>();
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
      addr = address;
   
      try {
         transport = new Transport(addr);
      } catch (IOException IOe) {
         throw new DBusException("Failed to connect to bus "+IOe.getMessage());
      }

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
      DBusSigHandler h = new _sighandler();
      addSigHandler(org.freedesktop.DBus.Local.Disconnected.class, h);
      addSigHandler(org.freedesktop.DBus.NameAcquired.class, h);
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
       * @throws DBusException If busname or objectpath are incorrectly formatted or type is not in a package.
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
       * @throws DBusException If busname or objectpath are incorrectly formatted or type is not in a package.
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
       * @throws DBusException If busname or objectpath are incorrectly formatted or type is not in a package.
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
       * @throws DBusException If busname or objectpath are incorrectly formatted or type is not in a package.
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

      // don't let people import things which don't have a
      // valid D-Bus interface name
      if (type.getName().equals(type.getSimpleName()))
         throw new DBusException("DBusInterfaces cannot be declared outside a package");
      
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
               try {
                  transport.disconnect();
               } catch (IOException IOe) {}
               conn.remove(addr);
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
         o = new _globalhandler(m.getPath());

      else {
         // now check for specific exported functions

         synchronized (exportedObjects) {
            eo = exportedObjects.get(m.getPath());
         }

         if (null == eo) {
            try {
               synchronized (outgoing) {
                  outgoing.add(new Error(m, new DBus.Error.UnknownObject(m.getPath()+" is not an object provided by this process."))); }
            } catch (DBusException DBe) {}
            return;
         }
         meth = eo.methods.get(new MethodTuple(m.getName(), m.getSig()));
         if (null == meth) {
            try {
               synchronized (outgoing) {
                  outgoing.add(new Error(m, new DBus.Error.UnknownMethod("The method `"+m.getInterface()+"."+m.getName()+"' does not exist on this object."))); }
            } catch (DBusException DBe) {}
            return;
         }
         o = eo.object;
      }

      // now execute it
      final Method me = meth;
      final Object ob = o;
      final EfficientQueue outqueue = outgoing;
      final boolean noreply = (1 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED));
      final DBusCallInfo info = new DBusCallInfo(m);
      final DBusConnection conn = this;
      addRunnable(new Runnable() 
      { 
         public void run() 
         { 
            try {
               Type[] ts = me.getGenericParameterTypes();
               m.args = Marshalling.deSerializeParameters(m.args, ts, conn);
            } catch (Exception e) {
               if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
               try {
                  synchronized (outqueue) {
                     outqueue.add(new Error(m, new DBus.Error.UnknownMethod("Failure in de-serializing message ("+e+")"))); 
                  }
               } catch (DBusException DBe) {} 
               return;
            }

            try { 
               synchronized (infomap) {
                  infomap.put(Thread.currentThread(), info);
               }
               Object result;
               try {
                  result = me.invoke(ob, m.args);
               } catch (InvocationTargetException ITe) {
                  if (DBusConnection.EXCEPTION_DEBUG) ITe.getCause().printStackTrace();
                  throw ITe.getCause();
               }
               synchronized (infomap) {
                  infomap.remove(Thread.currentThread());
               }
               if (!noreply) {
                  MethodReturn reply;
                  if (Void.TYPE.equals(me.getReturnType())) 
                     reply = new MethodReturn(m, null);
                  else {
                     StringBuffer sb = new StringBuffer();
                     for (String s: Marshalling.getDBusType(me. getGenericReturnType()))
                        sb.append(s);
                     reply = new MethodReturn(m, sb.toString(),result);
                  }
                  synchronized (outqueue) {
                     outqueue.add(reply);
                  }
               }
            } catch (DBusExecutionException DBEe) {
               if (DBusConnection.EXCEPTION_DEBUG) DBEe.printStackTrace();
               try {
                  synchronized (outqueue) {
                     outqueue.add(new Error(m, DBEe)); 
                  }
               } catch (DBusException DBe) {}
            } catch (Throwable e) {
               if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
               try { 
                  synchronized (outqueue) {
                     outqueue.add(new Error(m, new DBusExecutionException("Error Executing Method "+m.getInterface()+"."+m.getName()+": "+e.getMessage()))); 
                  }
               } catch (DBusException DBe) {}
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
         t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), null, null));
         if (null != t) v.addAll(t);
         t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), s.getPath(), null));
         if (null != t) v.addAll(t);
         t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), null, s.getSource()));
         if (null != t) v.addAll(t);
         t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), s.getPath(), s.getSource()));
         if (null != t) v.addAll(t);
      }
      if (0 == v.size()) return;
      final EfficientQueue outqueue = outgoing;
      for (final DBusSigHandler h: v)
         addRunnable(new Runnable() { public void run() {
            {
               try {
                  DBusSignal rs = s.createReal();
                  h.handle(rs); 
               } catch (DBusException DBe) {
                  if (DBusConnection.EXCEPTION_DEBUG) DBe.printStackTrace();
                  try {
                     synchronized (outqueue) {
                        outqueue.add(new Error(s, new DBusExecutionException("Error handling signal "+s.getInterface()+"."+s.getName()+": "+DBe.getMessage()))); 
                     }
                  } catch (DBusException DBe2) {}
               }
            }
         } });
   }
   private void handleMessage(final Error err)
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
   private void handleMessage(final MethodReturn mr)
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
         try {
            synchronized (outgoing) {
               outgoing.add(new Error(mr, new DBusExecutionException("Spurious reply. No message with the given serial id was awaiting a reply."))); 
            }
         } catch (DBusException DBe) {}
   }
   private void sendMessage(Message m)
   {
      try {
         transport.mout.writeMessage(m);
         if (m instanceof MethodCall) {
            if (0 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED))
               synchronized (pendingCalls) {
                  pendingCalls.put(m.getSerial(),(MethodCall) m);
               }
         }
      } catch (Exception e) {
         if (DBusConnection.EXCEPTION_DEBUG) e.printStackTrace();
         if (m instanceof MethodCall && e instanceof DBusExecutionException) 
            try {
               ((MethodCall)m).setReply(new Error(m, e));
            } catch (DBusException DBe) {}
         else if (m instanceof MethodCall)
            try {
               ((MethodCall)m).setReply(new Error(m, new DBusExecutionException("Message Failed to Send: "+e.getMessage())));
            } catch (DBusException DBe) {}
         else if (m instanceof MethodReturn)
            try {
               transport.mout.writeMessage(new Error(m, e));
            } catch(IOException IOe) {
            } catch(DBusException IOe) {}
      }
   }
   private Message readIncoming(int timeoutms, EfficientQueue outgoing) throws IOException, DBusException
   {
      // TODO do something with timeoutms and outgoing
      Message m = transport.min.readMessage();
      return m;
   }
}
