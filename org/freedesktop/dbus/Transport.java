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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
   private String findCookie(String context, String ID) throws IOException
   {
      String homedir = System.getProperty("user.home");
      File f = new File(homedir+"/.dbus-keyrings/"+context);
      BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      String s = null;
      String cookie = null;
      while (null != (s = r.readLine())) {
         String[] line = s.split(" ");
         if (line[0].equals(ID)) {
            cookie = line[2];
            break;
         }
      }
      return cookie;
   }
   /**
    * Takes the string, encodes it as hex and then turns it into a string again.
    * No, I don't know why either.
    */
   private String stupidlyEncode(String data)
   {
      return Hexdump.toHex(data.getBytes()).replaceAll(" ","");
   }
   private String stupidlyEncode(byte[] data)
   {
      return Hexdump.toHex(data).replaceAll(" ","");
   }
   private byte getNibble(char c)
   {
      switch (c) {
         case '0':
         case '1':
         case '2':
         case '3':
         case '4':
         case '5':
         case '6':
         case '7':
         case '8':
         case '9':
            return (byte) (c-'0');
         case 'A':
         case 'B':
         case 'C':
         case 'D':
         case 'E':
         case 'F':
            return (byte) (c-'A'+10);
         case 'a':
         case 'b':
         case 'c':
         case 'd':
         case 'e':
         case 'f':
            return (byte) (c-'a'+10);
         default:
            return 0;
      }
   }
   private String stupidlyDecode(String data)
   {
      char[] cs = new char[data.length()];
      char[] res = new char[cs.length/2];
      data.getChars(0, data.length(), cs, 0);
      for (int i = 0, j = 0; j < res.length; i += 2, j++) {
         int b = 0;
         b |= getNibble(cs[i])<<4;
         b |= getNibble(cs[i+1]);
         res[j] = (char) b;
      }
      return new String(res);
   }
   private boolean auth(BusAddress address, OutputStream out, InputStream in) throws IOException
   {
      out.write(new byte[] { 0 });
      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      UnixSystem uns = new UnixSystem();
      long uid = uns.getUid();
      String Uid = stupidlyEncode(""+uid);
      Collator col = Collator.getInstance();
      col.setDecomposition(Collator.FULL_DECOMPOSITION);
      col.setStrength(Collator.PRIMARY);
      if ("unix".equals(address.getType())) {
         if (null == address.getParameter("listen")) {
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
            if (Debug.debug) Debug.print(Debug.VERBOSE, "reading "+s);
            if ("BEGIN".equals(s)) return true;
         }
      } else {
         out.write(("AUTH DBUS_COOKIE_SHA1 "+Uid+"\r\n").getBytes());
         if (Debug.debug) Debug.print(Debug.VERBOSE, "AUTH DBUS_COOKIE_SHA1 "+Uid+"\r\n");
         String s = r.readLine();
         if (Debug.debug) Debug.print(Debug.VERBOSE, "reading: "+s);
         String[] reply=s.split(" ");
         if (0 != col.compare("DATA", reply[0])) return false;
         s = stupidlyDecode(reply[1]);
         if (Debug.debug) Debug.print(Debug.VERBOSE, "decoded: "+s);
         reply=s.split(" ");
         if (3 != reply.length) return false;
         String context = reply[0];
         String ID = reply[1];
         String serverchallenge = reply[2];
         MessageDigest md = null;
         try {
            md = MessageDigest.getInstance("SHA");
         } catch (NoSuchAlgorithmException NSAe) {
            if (Debug.debug && AbstractConnection.EXCEPTION_DEBUG) Debug.print(Debug.ERR, NSAe);
            return false; 
         }
         byte[] buf = new byte[8];
         Message.marshallintBig(System.currentTimeMillis(), buf, 0, 8);
         String clientchallenge = stupidlyEncode(md.digest(buf));
         md.reset();
         String cookie = findCookie(context, ID);
         if (null == cookie) return false;
         String response = serverchallenge+":"+clientchallenge+":"+cookie;
         response = stupidlyEncode(md.digest(response.getBytes("UTF-8")));

         out.write(("DATA "+stupidlyEncode(clientchallenge+" "+response)+"\r\n").getBytes());
         if (Debug.debug) Debug.print(Debug.VERBOSE, "DATA "+stupidlyEncode(clientchallenge+" "+response));
      }
      String s = r.readLine();
      if (Debug.debug) Debug.print(Debug.VERBOSE, s);
      String[] reply=s.split(" ");
      if (0 != col.compare("OK", reply[0])) return false;
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
         throw new IOException("unknown address type "+address.getType());
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


