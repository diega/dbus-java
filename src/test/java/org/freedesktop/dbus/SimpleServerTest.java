package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.TestSignalInterface;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleServerTest {

	private static DBusConnection connection;

	@BeforeClass
	public static void openConnection() throws DBusException {
		connection = DBusConnection.getConnection(DBusConnection.SESSION);
	}

	@Test
	public void connectionTest() throws DBusException {
		connection.addSigHandler(TestSignalInterface.TestSignal.class, new DBusSigHandler<TestSignalInterface.TestSignal>() {

			public void handle(TestSignalInterface.TestSignal s) {
				System.out.println(s.getPath());
			}
		});

		connection.sendSignal(new TestSignalInterface.TestSignal(  
                "/foo/bar/com/Wibble",  
                "Bar",  
                new UInt32(42)));

	}
}
