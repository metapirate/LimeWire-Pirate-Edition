/*******************************************************************************

	File:		FolderDialog.java
	Author:		Steve Roy
	Copyright:	Copyright (c) 2003-2007 Steve Roy <sroy@mac.com>
				
	Part of MRJ Adapter, a unified API for easy integration of Mac OS specific
	functionality within your cross-platform Java application.
	
	This library is open source and can be modified and/or distributed under
	the terms of the Artistic License.
	<http://homepage.mac.com/sroy/mrjadapter/license.html>
	
	Change History:
	03/06/03	Created this file - Steve
	03/25/03	Moved to the net.roydesign.ui package, modified to use the
				apple.awt.fileDialogForDirectories property with MRJ 4, added
				the getInitialMode() method, removed getFolder() because
				it's redundant with getDirectory(), removed the filename filter
				which was irrelevant - Steve
	12/16/03    Fixed getDirectory() to check if super.getFile() is null before
				trying to build a path with it - Steve

*******************************************************************************/

package org.limewire.ui.swing.util;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.Properties; 

/**
 * A folder dialog is a modal file dialog to specifically select a folder on
 * disk. This class takes advantage of a little know trick in Apple's VMs to
 * show a real folder dialog, with a Choose button and all. However, there is
 * no such thing on other platforms, where this class employs the usual
 * kludge which is to show a Save dialog. If you would rather use the Swing
 * JFileChooser, go right ahead.
 * 
 * @version MRJ Adapter 1.1
 */
public class OSXFolderDialog extends FileDialog
{
	/**
	 * Whether the <code>setMode()</code> method should check calls or not.
	 */
	private boolean modeCheckingEnabled = false;
	
	/**
	 * The version number of the Java runtime environment on OS-X.  This will remain -1 if we're not on OS-X.
	 */
	private static float mrjVersion = -1.f;
		
	/**
	 * Construct a folder dialog with the given parent frame.
	 * @param parent the parent frame
	 */
	public OSXFolderDialog(Frame parent)
	{
		this(parent, "");
		
		mrjVersion = loadMRJVersion();
	}
	
	/**
	 * Construct a folder dialog with the given parent frame and
	 * title.
	 * @param parent the parent frame
	 * @param title the title of the dialog
	 */
	public OSXFolderDialog(Frame parent, String title)
	{
		super(parent, title, getInitialMode());

		mrjVersion = loadMRJVersion();
		if (mrjVersion == -1.0f)
			setFile("-");

		modeCheckingEnabled = true;
	}
	
	/**
	 * Get the file of this file dialog, which in the case of this class,
	 * is always an empty string ("") unless the user has canceled where the
	 * return value will be <code>null</code>.
	 * @return an empty string if a directory was selected, or <code>null</code>
	 */
	@Override
    public String getFile()
	{
		// MRJ 2 returns "", MRJ 3 and 4 return the folder name, and other
		// platforms return the filename, so let's normalize this
		return super.getFile() != null ? "" : null;
	}
	
	/**
	 * Get the directory of this file dialog.
	 * @return the directory of the dialog, or null
	 */
	@Override
    public String getDirectory()
	{
		// MRJ 2 returns the folder, MRJ 3 and 4 return the parent folder, and
		// other platforms return the folder, so let's normalize this
		String path = super.getDirectory();
		if (path == null)
			return null;
		if (mrjVersion >= 3.0f && super.getFile() != null)
			return new File(path, super.getFile()).getPath();
		return path;
	}
	
	/**
	 * Set the mode of the dialog. This method is overriden because it
	 * doesn't make sense in the context of an application dialog to allow
	 * selection of the mode. It will throw an error if you try to call it.
	 * @param mode the mode
	 */
	@Override
    public void setMode(int mode)
	{
		if (modeCheckingEnabled)
			throw new Error("can't set mode");
		super.setMode(mode);
	}
	
	/**
	 * Make the dialog visible. Since the dialog is modal, this method
	 * will not return until either the user dismisses the dialog or
	 * you make it invisible yourself via <code>setVisible(false)</code>
	 * or <code>dispose()</code>.
	 */
	@SuppressWarnings("deprecation")
    @Override
    public void show()
	{
		// Set the system property required by Mac OS X
		String prop = null;
		if (mrjVersion >= 4.0f)
			prop = "apple.awt.fileDialogForDirectories";
		Properties props = System.getProperties();
		Object oldValue = null;
		if (prop != null)
		{
			oldValue = props.get(prop);
			props.put(prop, "true");
		}
		
		// Do the usual thing
		super.show();
		
		// Reset the system property
		if (prop != null)
		{
			if (oldValue == null)
				props.remove(prop);
			else
				props.put(prop, oldValue);
		}
	}
	
	/**
	 * Perform the preparatory setup for the folder dialog and return
	 * the value to use for the mode of the dialog. This method is called
	 * by the constructor.
	 * @return the mode value to use
	 */
	private static int getInitialMode()
	{
	    mrjVersion = loadMRJVersion();
	    
		if (mrjVersion >= 4.0f)
			return LOAD;
		else if (mrjVersion != -1.0f)
			return 3; // Any value >= 2 seems to work, but 3 is the commonly accepted value
		return SAVE;
	}
	
	private static float loadMRJVersion()
	{
	    String prop = System.getProperty("mrj.version");
		if (prop != null)
		{
			// 10.4, Java 1.4: 269
			// 10.4, Java 1.5: 1040.1.5.0_07-164
			// 10.5, Java 1.4: b05-302
			// 10.5, Java 1.5: 1040.1.5.0_13-237
			
			// Take all characters after the dash up to the second period,
			// if any, and convert that into a float
			int st = 0;
			int dash = prop.indexOf('-');
			if (dash != -1 && dash != prop.length() - 1)
				st = dash + 1;
			int en = prop.length();
			int dot = prop.indexOf('.', st);
			if (dot != -1 && dot != prop.length() - 1)
				dot = prop.indexOf('.', dot + 1);
			if (dot != -1)
				en = dot;
			mrjVersion = new Float(prop.substring(st, en)).floatValue();
		}
		
		return mrjVersion;
	}
}
