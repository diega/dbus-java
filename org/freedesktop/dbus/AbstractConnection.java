/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import java.util.regex.Pattern;

import org.freedesktop.DBus;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.FatalDBusException;
import org.freedesktop.dbus.exceptions.FatalException;
import org.freedesktop.dbus.exceptions.NonFatalException;

import cx.ath.matthew.debug.Debug;


/** Handles a connection to DBus.
 */
public abstract class AbstractConnection
{
   protected class _thread extends Thread
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
                     if (Debug.debug) Debug.print(Debug.VERBOSE, "Got Incoming Message: "+m);
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
               } catch (Exception e) { 
                  if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);            
                  if (e instanceof FatalException) {
                     try {
                        handleMessage(new org.freedesktop.DBus.Local.Disconnected("/"));
                     } catch (Exception ee) {
                        if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, ee);            
                     }
                     disconnect();
                  }
               }

               // write to the wire
               if (null != outgoing) synchronized (outgoing) {
                  if (!outgoing.isEmpty())
                     m = outgoing.remove(); }
               while (null != m) {
                  sendMessage(m);
                  m = null;
                  if (null != outgoing) synchronized (outgoing) {
                     if (!outgoing.isEmpty())
                        m = outgoing.remove(); }
               }
            }
            if (null != outgoing) synchronized (outgoing) {
               if (!outgoing.isEmpty())
                  m = outgoing.remove(); 
            }
            while (null != m) {
               sendMessage(m);
               m = null;
               if (null != outgoing) synchronized (outgoing) {
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
   protected class _workerthread extends Thread
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
   protected Map<String,ExportedObject> exportedObjects;
   private ObjectTree objectTree;
   protected Map<DBusInterface,RemoteObject> importedObjects;
   protected Map<SignalTuple,Vector<DBusSigHandler>> handledSignals;
   protected EfficientMap pendingCalls;
   protected LinkedList<Runnable> runnables;
   protected LinkedList<_workerthread> workers;
   protected boolean _run;
   EfficientQueue outgoing;
   LinkedList<Error> pendingErrors;
   private static final Map<Thread,DBusCallInfo> infomap = new HashMap<Thread,DBusCallInfo>();
   protected _thread thread;
   protected Transport transport;
   protected String addr;
   static final Pattern dollar_pattern = Pattern.compile("[$]");
   public static final boolean EXCEPTION_DEBUG;
   static final boolean FLOAT_SUPPORT;
   static {
      FLOAT_SUPPORT = (null != System.getenv("DBUS_JAVA_FLOATS"));
      EXCEPTION_DEBUG = (null != System.getenv("DBUS_JAVA_EXCEPTION_DEBUG"));
      if (EXCEPTION_DEBUG) {
         Debug.print("Debugging of internal exceptions enabled");
         Debug.setThrowableTraces(true);
      }
      if (Debug.debug) {
         File f = new File("debug.conf");
         if (f.exists()) {
            Debug.print("Loading debug config file: "+f);
            try {
               Debug.loadConfig(f);
            } catch (IOException IOe) {}
         } else {
            Properties p = new Properties();
            p.setProperty("ALL", "INFO");
            Debug.print("debug config file "+f+" does not exist, not loading.");
         }
         Debug.setHexDump(true);
      }
   }

   protected AbstractConnection(String address) throws DBusException
   {
      exportedObjects = new HashMap<String,ExportedObject>();
      importedObjects = new HashMap<DBusInterface,RemoteObject>();
      exportedObjects.put(null, new ExportedObject(new _globalhandler()));
      handledSignals = new HashMap<SignalTuple,Vector<DBusSigHandler>>();
      pendingCalls = new EfficientMap(PENDING_MAP_INITIAL_SIZE);
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
      addr = address;
   }

   protected void listen()
   {
      // start listening
      thread = new _thread();
      thread.start();
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

   abstract DBusInterface getExportedObject(String source, String path) throws DBusException;

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
   /** 
    * Send a signal.
    * @param signal The signal to send.
    */
   public void sendSignal(DBusSignal signal)
   {
      if (null == outgoing) return;
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

   protected abstract <T extends DBusSignal> void removeSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;
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

   protected abstract <T extends DBusSignal> void addSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;

   protected void addSigHandlerWithoutMatch(Class signal, DBusSigHandler handler) throws DBusException
   {
      DBusMatchRule rule = new DBusMatchRule(signal);
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
    */
   public void disconnect()
   {
      // run all pending tasks.
      while (runnables.size() > 0)
         synchronized (runnables) {
            runnables.notifyAll();
         }

      // stop the main thread
      _run = false;

      // disconnect from the trasport layer
      try {
         transport.disconnect();
      } catch (IOException IOe) {
         if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, IOe);            
      }

      // stop all the workers
      synchronized(workers) {
         for (_workerthread t: workers)
            t.halt();
      }

      // make sure none are blocking on the runnables queue still
      synchronized (runnables) {
         runnables.notifyAll();
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
         if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBEe);
         throw DBEe;
      } catch (Exception e) {
         if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
         throw new DBusExecutionException(e.getMessage());
      }
   }
   
   private void handleMessage(final MethodCall m) throws DBusException
   {
      if (Debug.debug) Debug.print(Debug.ERR, "Handling incoming method call: "+m);
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
               if (null != outgoing) synchronized (outgoing) {
                  outgoing.add(new Error(m, new DBus.Error.UnknownObject(m.getPath()+" is not an object provided by this process."))); }
            } catch (DBusException DBe) {}
            return;
         }
         meth = eo.methods.get(new MethodTuple(m.getName(), m.getSig()));
         if (null == meth) {
            try {
               if (null != outgoing) synchronized (outgoing) {
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
      final AbstractConnection conn = this;
      addRunnable(new Runnable() 
      { 
         public void run() 
         { 
            if (Debug.debug) Debug.print(Debug.DEBUG, "Running method "+me+" for remote call");
            try {
               Type[] ts = me.getGenericParameterTypes();
               m.setArgs(Marshalling.deSerializeParameters(m.getParameters(), ts, conn));
               if (Debug.debug) Debug.print(Debug.VERBOSE, "Deserialised "+Arrays.deepToString(m.getParameters())+" to types "+Arrays.deepToString(ts));
            } catch (Exception e) {
               if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
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
                  result = me.invoke(ob, m.getParameters());
               } catch (InvocationTargetException ITe) {
                  if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, ITe.getCause());
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
                     for (String s: Marshalling.getDBusType(me.getGenericReturnType()))
                        sb.append(s);
                     Object[] nr = Marshalling.convertParameters(new Object[] { result }, new Type[] {me.getGenericReturnType()}, conn);
                     
                     reply = new MethodReturn(m, sb.toString(),nr);
                  }
                  synchronized (outqueue) {
                     if (Debug.debug) Debug.print(Debug.VERBOSE, "Queuing reply");
                     outqueue.add(reply);
                  }
               }
            } catch (DBusExecutionException DBEe) {
               if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBEe);
               try {
                  synchronized (outqueue) {
                     outqueue.add(new Error(m, DBEe)); 
                  }
               } catch (DBusException DBe) {}
            } catch (Throwable e) {
               if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
               try { 
                  synchronized (outqueue) {
                     outqueue.add(new Error(m, new DBusExecutionException("Error Executing Method "+m.getInterface()+"."+m.getName()+": "+e.getMessage()))); 
                  }
               } catch (DBusException DBe) {}
            } 
         }
      });
   }
   @SuppressWarnings({"unchecked","deprecation"})
   private void handleMessage(final DBusSignal s)
   {
      if (Debug.debug) Debug.print(Debug.ERR, "Handling incoming signal: "+s);
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
      final AbstractConnection conn = this;
      for (final DBusSigHandler h: v)
         addRunnable(new Runnable() { public void run() {
            {
               try {
                  DBusSignal rs;
                  if (s instanceof DBusSignal.internalsig || s.getClass().equals(DBusSignal.class))
                     rs = s.createReal(conn);
                  else
                     rs = s;
                  h.handle(rs); 
               } catch (DBusException DBe) {
                  if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, DBe);
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
      if (Debug.debug) Debug.print(Debug.ERR, "Handling incoming error: "+err);
      MethodCall m = null;
      if (null == pendingCalls) return;
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
      if (Debug.debug) Debug.print(Debug.ERR, "Handling incoming method return: "+mr);
      MethodCall m = null;
      if (null == pendingCalls) return;
      synchronized (pendingCalls) {
         if (pendingCalls.contains(mr.getReplySerial()))
            m = pendingCalls.remove(mr.getReplySerial());
      }
      if (null != m) {
         m.setReply(mr);
         mr.setCall(m);
      } else
         try {
            if (null != outgoing) synchronized (outgoing) {
               outgoing.add(new Error(mr, new DBusExecutionException("Spurious reply. No message with the given serial id was awaiting a reply."))); 
            }
         } catch (DBusException DBe) {}
   }
   protected void sendMessage(Message m)
   {
      try {
         if (m instanceof DBusSignal) 
            ((DBusSignal) m).appendbody(this);

         transport.mout.writeMessage(m);
         
         if (m instanceof MethodCall) {
            if (0 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED))
               if (null == pendingCalls) 
                  ((MethodCall) m).setReply(new Error("org.freedesktop.DBus.Local", "org.freedesktop.DBus.Local.Disconnected", 0, "s", new Object[] { "Disconnected" }));
               else synchronized (pendingCalls) {
                  pendingCalls.put(m.getSerial(),(MethodCall) m);
               }
         }
      } catch (Exception e) {
         if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);
         if (m instanceof MethodCall && e instanceof DBusExecutionException) 
            try {
               ((MethodCall)m).setReply(new Error(m, e));
            } catch (DBusException DBe) {}
         else if (m instanceof MethodCall)
            try {
               if (Debug.debug) Debug.print(Debug.INFO, "Setting reply to "+m+" as an error");
               ((MethodCall)m).setReply(new Error(m, new DBusExecutionException("Message Failed to Send: "+e.getMessage())));
            } catch (DBusException DBe) {}
         else if (m instanceof MethodReturn)
            try {
               transport.mout.writeMessage(new Error(m, e));
            } catch(IOException IOe) {
               if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, IOe);            
            } catch(DBusException IOe) {
               if (EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, e);            
            }
      }
   }
   private Message readIncoming(int timeoutms, EfficientQueue outgoing) throws DBusException 
   {
      if (null == transport) throw new NotConnected("No transport present");
      // TODO do something with timeoutms and outgoing
      Message m = null;
      try {
         m = transport.min.readMessage();
      } catch (IOException IOe) {
         throw new FatalDBusException(IOe.getMessage());
      }
      return m;
   }
   /**
    * Returns the address this connection is connected to.
    */
   public BusAddress getAddress() { return new BusAddress(addr); }
}
