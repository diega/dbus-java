package org.freedesktop.dbus.test;
import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;
import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.MethodCall;
import org.freedesktop.dbus.Transport;

public class test_low_level
{
   public static void main(String[] args) throws Exception
   {
      Debug.setHexDump(true);
      String addr = System.getenv("DBUS_SESSION_BUS_ADDRESS");
      Debug.print(addr);
      BusAddress address = new BusAddress(addr);
      Debug.print(address);

      Transport conn = new Transport(address);

      Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m.getClass());
      Debug.print(m);
      m = conn.min.readMessage();
      Debug.print(m.getClass());
      Debug.print(m);
      m = conn.min.readMessage();
      Debug.print(""+m);
      m = new MethodCall("org.freedesktop.DBus", "/", null, "Hello", (byte) 0, null);
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m);

      m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "RequestName", (byte) 0, "su", "org.testname", 0);
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m);
      m = new DBusSignal(null, "/foo", "org.foo", "Foo", null);
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m);
      conn.disconnect();
   }
}
