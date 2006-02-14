package org.freedesktop.dbus.viewer;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.swing.JTabbedPane;

class SaveAllAction extends TabbedSaveAction
{

	private class TabIterator implements Iterator<TextFile>
	{
		private int i = 0;
		/** {@inheritDoc} */
		public boolean hasNext()
		{
			return i < tabbedPane.getTabCount();
		}

		/** {@inheritDoc} */
		public TextFile next()
		{
			if (hasNext())
			{
				int currentIndex = i;
				i++;
				return getTextFile(currentIndex);
			}
			throw new NoSuchElementException();
		}

		/** {@inheritDoc} */
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

	}

	protected SaveAllAction(JTabbedPane tabbedPane)
	{
		super(tabbedPane, "Save All...");
	}

	/** {@inheritDoc} */
	public Iterator<TextFile> iterator()
	{
		return new TabIterator();
	}
	
}
