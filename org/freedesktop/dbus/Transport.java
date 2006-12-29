/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.Collator;
import java.util.Random;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixServerSocket;
import cx.ath.matthew.unix.UnixSocketAddress;
import cx.ath.matthew.utils.Hexdump;
import cx.ath.matthew.debug.Debug;
import com.sun.security.auth.module.UnixSystem;

public class Transport
{
   private BusAddress address;
   public MessageReader min;
   public MessageWriter mout;
   public Transport() {}
   public static String genGUID()
   {
      Random r = new Random();
      byte[] buf = new byte[16];
      r.nextBytes(buf);
      String guid = Hexdump.toHex(buf);
      return guid.replaceAll(" ", "");
   }
   public Transport(BusAddress address) throws IOException
   {
      connect(address);
   }
   public Transport(String address) throws IOException
   {
      connect(new BusAddress(address));
   }
   private boolean auth(BusAddress address, OutputStream out, InputStream in) throws IOException
   {
      out.write(new byte[] { 0 });
      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      if ("unix".equals(address.getType())) {
         if (null == address.getParameter("listen")) {
            UnixSystem uns = new UnixSystem();
            long uid = uns.getUid();
            String Uid = Hexdump.toHex((""+uid).getBytes()).replaceAll(" ","");
            out.write(("AUTH EXTERNAL "+Uid+"\r\n").getBytes());
            if (Debug.debug) Debug.print(Debug.VERBOSE, "AUTH EXTERNAL "+Uid+"\r\n");
         } else {
            String s = r.readLine();
            if (Debug.debug) Debug.print(Debug.VERBOSE, "reading:"+s);
            String guid = address.getParameter("guid");
            if (null == guid) guid = genGUID();
            out.write(("OK "+guid+"\r\n").getBytes());
            if (Debug.debug) Debug.print(Debug.VERBOSE, "OK "+guid);
            s = r.readLine();
            if (Debug.debug) Debug.print(Debug.VERBOSE, "reading"+s);
            if ("BEGIN".equals(s)) return true;
         }
      } else {
         out.write(("AUTH DBUS_COOKIE_SHA1\r\n").getBytes());
      }
      String s = r.readLine();
      if (Debug.debug) Debug.print(Debug.VERBOSE, s);
      String[] reply=s.split(" ");
      Collator col = Collator.getInstance();
      col.setDecomposition(Collator.FULL_DECOMPOSITION);
      col.setStrength(Collator.PRIMARY);
      if (0 != col.compare("OK", reply[0])) {
         if (Debug.debug) Debug.print(Debug.VERBOSE, "reply[0] = `"+reply[0]+"'");
         return false;
      }
      if (null == address.getParameter("guid") || (reply.length > 1 && reply[1].equals(address.getParameter("guid")))) {
         if (Debug.debug) Debug.print(Debug.VERBOSE, "BEGIN\r\n");
         out.write("BEGIN\r\n".getBytes());
         return true;
      } else if (Debug.debug) 
         if (reply.length == 1)
            Debug.print(Debug.ERR, "GUID Mismatch, expecting: "+address.getParameter("guid")+", got no guid");
         else
            Debug.print(Debug.ERR, "GUID Mismatch, expecting: "+address.getParameter("guid")+", got: "+reply[1]);
      return false;
   }
   public void connect(String address) throws IOException
   {
      connect(new BusAddress(address));
   }
   public void connect(BusAddress address) throws IOException
   {
      if (Debug.debug) Debug.print(Debug.INFO, "Connecting to "+address);
      this.address = address;
      OutputStream out = null;
      InputStream in = null;
      UnixSocket us = null;
      if ("unix".equals(address.getType())) {
         if (null != address.getParameter("listen")) {
            UnixServerSocket uss = new UnixServerSocket();
            if (null != address.getParameter("abstract"))
               uss.bind(new UnixSocketAddress(address.getParameter("abstract"), true));
            else if (null != address.getParameter("path"))
               uss.bind(new UnixSocketAddress(address.getParameter("path"), false));
            us = uss.accept();
         } else {
            us = new UnixSocket();
            if (null != address.getParameter("abstract"))
               us.connect(new UnixSocketAddress(address.getParameter("abstract"), true));
            else if (null != address.getParameter("path"))
               us.connect(new UnixSocketAddress(address.getParameter("path"), false));
         }
         us.setPassCred(true);
         in = us.getInputStream();
         out = us.getOutputStream();
      } else if ("tcp".equals(address.getType())) {
         Socket s = null;
         if (null != address.getParameter("listen")) {
            ServerSocket ss = new ServerSocket();
            ss.bind(new InetSocketAddress(address.getParameter("host"), Integer.parseInt(address.getParameter("port"))));
            s = ss.accept();
         } else {
            s = new Socket();
            s.connect(new InetSocketAddress(address.getParameter("host"), Integer.parseInt(address.getParameter("port"))));
         }
         s.setSoTimeout(0);
         in = s.getInputStream();
         out = s.getOutputStream();
      } else {
         System.err.println("unknown address type");
         System.exit(1);
      }
      
      if (!auth(address, out, in)) {
         out.close();
         throw new IOException("Failed to auth");
      }
      if (null != us) {
         if (Debug.debug) Debug.print(Debug.VERBOSE, "Setting non-blocking on UnixSocket");
         us.setBlocking(false);
      }
      mout = new MessageWriter(out);
      min = new MessageReader(in);
   }
   public void disconnect() throws IOException
   {
      min.close();
      mout.close();
   }
}


