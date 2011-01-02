package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.Before;
import org.junit.Test;

public class SimpleServerTest {

	@Before
	public void openConnection() throws DBusException {
		System.out.println(System.getProperty("java.library.path"));
		DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
	}

	@Test
	public void connectionTest() {
		System.out.println("test");
	}
}
