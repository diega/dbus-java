package test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import cx.ath.matthew.unix.UnixSocket;
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
   public Transport(BusAddress address) throws IOException
   {
      connect(address);
   }
   private boolean auth(BusAddress address, OutputStream out, InputStream in) throws IOException
   {
      out.write(new byte[] { 0 });
      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      if ("unix".equals(address.getType())) {
         UnixSystem uns = new UnixSystem();
         long uid = uns.getUid();
         String Uid = Hexdump.toHex((""+uid).getBytes()).replaceAll(" ","");
         out.write(("AUTH EXTERNAL "+Uid+"\r\n").getBytes());
      } else {
         out.write(("AUTH DBUS_COOKIE_SHA1\r\n").getBytes());
      }
      String s = r.readLine();
      Debug.print(s);
      String[] reply=s.split(" ");
      if (!"OK".equals(reply[0])) return false;
      if (null == address.getParameter("guid") || reply[1].equals(address.getParameter("guid"))) {
         out.write("BEGIN\r\n".getBytes());
         return true;
      } 
      return false;
   }
   public void connect(BusAddress address) throws IOException
   {
      this.address = address;
      OutputStream out = null;
      InputStream in = null;
      if ("unix".equals(address.getType())) {
         UnixSocket us = new UnixSocket();
         if (null != address.getParameter("abstract"))
            us.connect(new UnixSocketAddress(address.getParameter("abstract"), true));
         else if (null != address.getParameter("path"))
            us.connect(new UnixSocketAddress(address.getParameter("path"), false));
         us.setPassCred(true);
         in = us.getInputStream();
         out = us.getOutputStream();
      } else if ("tcp".equals(address.getType())) {
         Socket s = new Socket();
         s.connect(new InetSocketAddress(address.getParameter("host"), Integer.parseInt(address.getParameter("port"))));
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
      mout = new MessageWriter(out);
      min = new MessageReader(in);
   }
}


