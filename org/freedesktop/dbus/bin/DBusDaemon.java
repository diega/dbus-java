/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.bin;

import org.freedesktop.DBus;
import org.freedesktop.dbus.AbstractConnection;
import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.DirectConnection;
import org.freedesktop.dbus.Error;
import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.MessageReader;
import org.freedesktop.dbus.MessageWriter;
import org.freedesktop.dbus.MethodCall;
import org.freedesktop.dbus.MethodReturn;
import org.freedesktop.dbus.Transport;
import org.freedesktop.dbus.exceptions.DBusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.unix.UnixServerSocket;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixSocketAddress;

/**
 * A replacement DBusDaemon
 */
public class DBusDaemon extends Thread
{
   static class Connstruct
   {
      public UnixSocket sock;
      public MessageReader min;
      public MessageWriter mout;
      public String unique;
      public Connstruct(UnixSocket sock)
      {
         this.sock = sock;
         min = new MessageReader(sock.getInputStream());
         mout = new MessageWriter(sock.getOutputStream());
      }
   }
   private Vector<Connstruct> conns = new Vector<Connstruct>();
   private HashMap<String, Connstruct> names = new HashMap<String, Connstruct>();
   private HashMap<Connstruct, Queue<Message>> queues = new HashMap<Connstruct, Queue<Message>>();
   private boolean _run = true;
   private int next_unique = 0;
   private Object unique_lock = new Object();
   public DBusDaemon()
   {
   }
   private void send(Connstruct c, Message m)
   {
      if (Debug.debug){
         if (null == c)
            Debug.print(Debug.DEBUG, "Queing message "+m+" for all connections");
         else
            Debug.print(Debug.DEBUG, "Queing message "+m+" for "+c.unique);
      }
      // send to all connections
      if (null == c) {
         Map<Connstruct, Queue<Message>> local;
         synchronized (queues) {
            local = (Map<Connstruct, Queue<Message>>) queues.clone();
         }
         for (Queue<Message> q: local.values()) {
            synchronized (q) {
               q.offer(m);
            }
         }
      } else {
         Queue<Message> q;
         synchronized (queues) {
            q = queues.get(c);
         }
         synchronized (q) {
            q.offer(m);
         }
      }
   }
   private void serverHandleMessage(Connstruct c, Message m) throws DBusException
   {
      if (Debug.debug) Debug.print(Debug.DEBUG, "Handling message "+m+" from "+c.unique);
      if (!(m instanceof MethodCall)) return;
      if ("Hello".equals(m.getName())) {
         synchronized (c) {
            if (null != c.unique) {
               send(c, new Error(c.unique, "org.freedesktop.DBus.Error.AccessDenied", m.getSerial(), "s", "Connection has already sent a Hello message"));
               return;
            }

            synchronized (unique_lock) {
               c.unique = ":1."+(++next_unique);
            }
         }
         synchronized (names) {
            names.put(c.unique, c);
         }
         if (Debug.debug) Debug.print(Debug.DEBUG, "Client "+c.unique+" registered");
         send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, "s", c.unique));
         send(c, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameAcquired", "s", c.unique));
         send(null, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop. DBus", "NameOwnerChanged", "sss", c.unique, "", c.unique));
      }
      else if ("ListNames".equals(m.getName())) {
         String[] ns;
         synchronized (names) {
            ns = names.keySet().toArray(new String[0]);
         }
         send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, "as", (Object) ns));
      } 
      else if ("AddMatch".equals(m.getName())) {
         send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, null));
      }
      else if ("RemoveMatch".equals(m.getName())) {
         send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, null));
      } 
      else if ("GetNameOwner".equals(m.getName())) {
         Object[] args = m.getParameters();
         if (null == args || args.length < 1 || !(args[0] instanceof String)) {
            send(c,new Error("org.freedesktop.DBus", c.unique, "org.freedesktop.DBus.Error.GeneralError", m.getSerial(), "s", "GetNameOwner arguments invalid"));
            return;
         }
         Connstruct owner = names.get((String) args[0]);
         if (null == owner) 
            send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, "s", ""));
         else
            send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, "s", owner.unique));
      } 
      else if ("RequestName".equals(m.getName())) {
         Object[] args = m.getParameters();
         if (null == args || args.length < 2 || !(args[0] instanceof String)) {
            send(c,new Error("org.freedesktop.DBus", c.unique, "org.freedesktop.DBus.Error.GeneralError", m.getSerial(), "s", "RequestName arguments invalid"));
            return;
         }
         boolean exists = false;
         synchronized (names) {
            if (!(exists = names.keySet().contains((String) args[0])))
               names.put((String) args[0], c);
         }
         if (exists) {
            send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, "u", DBus.DBUS_REQUEST_NAME_REPLY_EXISTS));
            return;
         }
         send(c, new MethodReturn("org.freedesktop.DBus", (MethodCall) m, "u", DBus.DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER));
         send(c, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop. DBus", "NameAcquired", "s", (String) args[0]));
         send(null, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop. DBus", "NameOwnerChanged", "sss", (String) args[0], "", c.unique));
      }

      // send an error
      else {
         send(c,new Error("org.freedesktop.DBus", c.unique, "org.freedesktop.DBus.Error.UnknownMethod", m.getSerial(), "s", "This service does not support the "+m.getName()+" Method"));
      }
   }
   private List<Connstruct> findSignalMatches(DBusSignal sig)
   {
      synchronized (conns) {
         return (List<Connstruct>) conns.clone();
      }
   }
   public void run()
   {
      while (_run) {
         List<Connstruct> local;
         synchronized (conns) {
            local = (List<Connstruct>) conns.clone();
         }
         for (Connstruct c: local) try {
            Message m = c.min.readMessage();

            // check if we have a message
            if (null != m) {
               // check if they have hello'd
               if (null == c.unique 
                     && (!(m instanceof MethodCall) 
                        || !"org.freedesktop.DBus".equals(m.getDestination())
                        || !"Hello".equals(m.getName()))) {
                  send(c,new Error("org.freedesktop.DBus", null, "org.freedesktop.DBus.Error.AccessDenied", m.getSerial(), "s", "You must send a Hello message"));
               } else {

                  try {
                     if (null != c.unique) m.setSource(c.unique);
                  } catch (DBusException DBe) {
                     if (Debug.debug && AbstractConnection.EXCEPTION_DEBUG) Debug.print(Debug.ERR, DBe);
                     send(c,new Error("org.freedesktop.DBus", null, "org.freedesktop.DBus.Error.GeneralError", m.getSerial(), "s", "Sending message failed"));
                  }

                  if ("org.freedesktop.DBus".equals(m.getDestination())) {
                     serverHandleMessage(c, m);
                  } else {
                     if (m instanceof DBusSignal) {
                        List<Connstruct> list = findSignalMatches((DBusSignal) m);
                        for (Connstruct d: list)
                                           send(d, m);
                     } else {
                        Connstruct dest = names.get(m.getDestination());

                        if (null == dest) {
                           send(c, new Error("org.freedesktop.DBus", null, "org.freedesktop.DBus.Error.ServiceUnknown", m.getSerial(), "s", "The name `"+m.getDestination()+"'does not exist"));
                        } else
                           send(dest, m);
                     }
                  }
               }
            }
            Queue<Message> q;
            synchronized (queues) {
               q = queues.get(c);
            }

            synchronized (q) {
               while (q.size() > 0) {
                  Message tosend = q.poll();
                  c.mout.writeMessage(tosend);
               }
            }
         }
         catch (IOException IOe) {
            if (Debug.debug && AbstractConnection.EXCEPTION_DEBUG) Debug.print(Debug.ERR, IOe);
            removeConnection(c);
         }
         catch (DBusException DBe) {
            if (Debug.debug && AbstractConnection.EXCEPTION_DEBUG) Debug.print(Debug.ERR, DBe);
         }
      }
   }
   private void removeConnection(Connstruct c)
   {
      try {
         c.sock.close();
      } catch (IOException IOe) {}
      synchronized(conns) {
         conns.remove(c);
      }
      synchronized (queues) {
         queues.remove(c);
      }
      synchronized(names) {
         List<String> toRemove = new Vector<String>();
         for (String name: names.keySet()) 
            if (names.get(name) == c) {
               toRemove.add(name);
               try {
                  send(null, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameOwnerChanged", "sss", name, c.unique, ""));
               } catch (DBusException DBe) {
                  if (Debug.debug && AbstractConnection.EXCEPTION_DEBUG) Debug.print(Debug.ERR, DBe);
               }
            }
         for (String name: toRemove)
            names.remove(name);
      }
   }
   public void addSock(UnixSocket us)
   {
      if (Debug.debug) Debug.print(Debug.INFO, "New Client");
      Connstruct c = new Connstruct(us);
      synchronized (conns) {
         conns.add(c);
      }
      synchronized (queues) {
         queues.put(c, new LinkedList<Message>());
      }
   }
   public static void syntax()
   {
      System.out.println("Syntax: DBusDaemon [--help] [-h]");
      System.exit(1);
   }
   public static void main(String args[]) throws Exception
   {
      boolean owners = false;
      boolean users = false;

      for (String a: args) 
         if ("--help".equals(a)) syntax();
         else if ("-h".equals(a)) syntax();
         else syntax();

      String addr = DirectConnection.createDynamicSession();
      System.out.println(addr);
      BusAddress address = new BusAddress(addr);
      UnixServerSocket uss = new UnixServerSocket(new UnixSocketAddress(address.getParameter("abstract"), true)); 
      DBusDaemon d = new DBusDaemon();
      d.start();
      while (true) {
         UnixSocket s = uss.accept();
         if ((new Transport.SASL()).auth(Transport.SASL.MODE_SERVER, Transport.SASL.AUTH_EXTERNAL, address.getParameter("guid"), s.getOutputStream(), s.getInputStream()))
            d.addSock(s);
         else
            s.close();
      }
   }
}
