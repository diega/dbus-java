package org.freedesktop.dbus.viewer;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class SaveFileAction extends TabbedSaveAction implements ChangeListener
{
	private class SelectedTabIterator implements Iterator<TextFile>
	{
		boolean iterated = false;
		/** {@inheritDoc} */
		public boolean hasNext()
		{
			return !iterated;
		}

		/** {@inheritDoc} */
		public TextFile next()
		{
			if (iterated)
			{
				throw new NoSuchElementException("Already iterated");
			}
			iterated = true;
			return getTextFile(tabbedPane.getSelectedIndex());
		}

		/** {@inheritDoc} */
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

	}

	SaveFileAction(JTabbedPane tabbedPane)
	{
		super(tabbedPane);
		
		enableAndSetName();
		
		tabbedPane.addChangeListener(this);
	}

	/** {@inheritDoc} */
	public void stateChanged(ChangeEvent e)
	{
		enableAndSetName();
	}

	/**
	 * Enable and set the name of the action based on the shown tab
	 */
	void enableAndSetName()
	{
		int selectedIndex = tabbedPane.getSelectedIndex();
		boolean enabled = selectedIndex > -1;
		putValue(Action.NAME, "Save " + getFileName(selectedIndex) + "...");
		setEnabled(enabled);
	}

	/** {@inheritDoc} */
	public Iterator<TextFile> iterator()
	{
		return new SelectedTabIterator();
	}
}
