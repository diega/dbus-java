package org.freedesktop.dbus.viewer;

/** A Text file abstraction
 * 
 *
 * @author pete
 * @since 10/02/2006
 */
class TextFile
{			
	final String fileName;
	final String contents;
	/** Create the TextFile
	 * 
	 * @param fileName The file name
	 * @param contents The contents
	 */
	public TextFile(String fileName, String contents)
	{
		this.fileName = fileName;
		this.contents = contents;
	}
	/** Retrieve the fileName
	 * 
	 * @return The fileName.
	 */
	String getFileName()
	{
		return fileName;
	}
	/** Retrieve the contents
	 * 
	 * @return The contents.
	 */
	String getContents()
	{
		return contents;
	}
}
