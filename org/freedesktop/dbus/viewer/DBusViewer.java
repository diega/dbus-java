/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.freedesktop.DBus;
import org.freedesktop.DBus.Introspectable;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * A viewer for DBus
 * 
 * goal is to replicate the functionality of kdbus with Java smarts
 * 
 * @author pete
 * @since 29/01/2006
 */
public class DBusViewer
{
	private static final Map<String, Integer> CONNECTION_TYPES = new HashMap<String, Integer>();

	static
	{
		CONNECTION_TYPES.put("System", DBusConnection.SYSTEM);
		CONNECTION_TYPES.put("Session", DBusConnection.SESSION);
	}

	/** Create the DBusViewer
	 * 
	 * @param connectionTypes The map of connection types
	 */
	public DBusViewer(final Map<String, Integer> connectionTypes)
	{
		connections = new ArrayList<DBusConnection>(connectionTypes.size());

		SwingUtilities.invokeLater(new Runnable()
		{
			@SuppressWarnings("synthetic-access")
			public void run()
			{

				final JTabbedPane tabbedPane = new JTabbedPane();
				addTabs(tabbedPane, connectionTypes);
				final JFrame frame = new JFrame("Dbus Viewer");
				frame.setContentPane(tabbedPane);
				frame.setSize(600, 400);
				frame.addWindowListener(new WindowAdapter()
				{
					@Override
					public void windowClosing(WindowEvent e)
					{
						frame.dispose();
						for (DBusConnection connection : connections)
						{
							connection.disconnect();
						}
						System.exit(0);
					}
				});
				frame.setVisible(true);
			}
		});
	}

	private List<DBusConnection> connections;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		new DBusViewer(CONNECTION_TYPES);
	}

	/** Add tabs for each supplied connection type
	 * @param tabbedPane The tabbed pane
	 * @param connectionTypes The connection
	 */
	private void addTabs(final JTabbedPane tabbedPane,
			final Map<String, Integer> connectionTypes)
	{
		for (final String key : connectionTypes.keySet())
		{
			final JLabel label = new JLabel("Processing DBus for " + key);
			tabbedPane.addTab(key, label);
		}
		Runnable loader = new Runnable()
		{
			@SuppressWarnings("synthetic-access")
			public void run()
			{
				boolean users = true, owners = true;
				for (final String key : connectionTypes.keySet())
				{
					try
					{
						DBusConnection conn = DBusConnection
								.getConnection(connectionTypes.get(key));
						connections.add(conn);

						final TableModel tableModel = listDBusConnection(users,
								owners, conn);

						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								int index = tabbedPane.indexOfTab(key);
								final JTable table = new JTable(tableModel);
								


								JScrollPane scrollPane = new JScrollPane(table);
								
								JPanel tab = new JPanel(new BorderLayout());
								tab.add(scrollPane, BorderLayout.CENTER);
								
								JPanel southPanel = new JPanel();
								final JButton button = new JButton(new IntrospectAction(table));
								southPanel.add(button);
								
								tab.add(southPanel, BorderLayout.SOUTH);
								
								tabbedPane.setComponentAt(index,
										tab);
								
							}
						});
					}
					catch (final DBusException e)
					{
						e.printStackTrace();
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								int index = tabbedPane.indexOfTab(key);
								JLabel label = (JLabel) tabbedPane
										.getComponentAt(index);
								label
										.setText("Could not load Dbus information for "
												+ key + ":" + e.getMessage());
							}
						});
					}
					catch (final DBusExecutionException e)
					{
						e.printStackTrace();
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								int index = tabbedPane.indexOfTab(key);
								JLabel label = (JLabel) tabbedPane
										.getComponentAt(index);
								label
										.setText("Could not load Dbus information for "
												+ key + ":" + e.getMessage());
							}
						});
					}
				}
			}
		};
		final Thread thread = new Thread(loader);
		thread.setName("DBus Loader");
		thread.start();
	}
	
	/* based on code from org.freedesktop.dbus.ListDBus */
	private DBusTableModel listDBusConnection(boolean users, boolean owners,
			DBusConnection conn) throws DBusException
	{
		DBusTableModel model = new DBusTableModel();

		DBus dbus = (DBus) conn.getRemoteObject("org.freedesktop.DBus",
				"/org/freedesktop/DBus", DBus.class);
		String[] names = dbus.ListNames();
		for (String name : names)
		{
			DBusEntry entry = new DBusEntry();
			entry.setName(name);
			try
			{
				//String objectpath = '/' + name.replace('.', '/');
            String objectpath = "/";
				Introspectable introspectable = (Introspectable) conn
						.getRemoteObject(name, objectpath, Introspectable.class);
				entry.setIntrospectable(introspectable);
			}
			catch (DBusException e)
			{
			}
			catch (DBusExecutionException e)
			{
			}

			if (users) try
			{
				final UInt32 user = dbus.GetConnectionUnixUser(name);
				entry.setUser(user);
			}
			catch (DBusExecutionException DBEe)
			{
			}
			if (!name.startsWith(":") && owners)
			{
				try
				{
					final String owner = dbus.GetNameOwner(name);
					entry.setOwner(owner);
				}
				catch (DBusExecutionException DBEe)
				{
				}
			}
			model.add(entry);
		}
		return model;
	}

}
