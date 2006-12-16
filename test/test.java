package test;
import cx.ath.matthew.debug.Debug;
import cx.ath.matthew.utils.Hexdump;

public class test
{
   public static void main(String[] args) throws Exception
   {
      Message test = new MethodCall(":1.0", "/", "org.foo", "Hiii", null);

      //System.exit(0);

      Debug.setHexDump(true);
      BusAddress address = new BusAddress(System.getenv("DBUS_SESSION_BUS_ADDRESS"));
      Debug.print(address);

      Transport conn = new Transport(address);

      Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", null);
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m);
      m = conn.min.readMessage();
      Debug.print(m);
      m = new MethodCall("org.freedesktop.DBus", "/", null, "Hello", null);
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m);

      m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "RequestName", "s", "org.testname");
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m);
      m = new Signal("/foo", "org.foo", "Foo", null);
      conn.mout.writeMessage(m);
      m = conn.min.readMessage();
      Debug.print(m);
   }
}
