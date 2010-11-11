package org.limewire.ui.swing.components;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JFrame;

import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;


/**
 * A JFrame that uses LimeWire's icon.
 */
public class LimeJFrame extends JFrame {
    
    private final LimeIconInfo iconInfo = new LimeIconInfo();
    
    public LimeJFrame() throws HeadlessException {
        super();
        initialize();
    }

    public LimeJFrame(GraphicsConfiguration gc) {
        super(gc);
        initialize();
    }

    public LimeJFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        initialize();
    }

    public LimeJFrame(String title) throws HeadlessException {
        super(title);
        initialize();
    }

    private void initialize() {
        // We set the icon image on every OS except Mac OS X, because on Mac setting this doesn't actually place an icon
        // in a corner of the frame; it's used instead as an icon for the minimized application. (LWC-4726)
        if(!OSUtils.isMacOSX())
            setIconImage(iconInfo.getImage());
    }

    // Overrides addNotify() to change to a platform specific icon right afterwards.
    @Override
	public void addNotify() {
		super.addNotify();
		SystemUtils.setWindowIcon(this, iconInfo.getIconFile());
		
        // The native call to setWindowIcon is causing issues on Windows 7. It appears
        // the OS is grabbing the wrong image from the ico or can't find the correct image
		// at times and uses the JFrame icon on occasions instead. Calling setIconImages
        // is similar to the native call in that we give the window a set of different size
        // icons and the OS can choose which is most appropriate to use.
	    if(OSUtils.isWindows7())
	        setIconImages(iconInfo.getIconImages());
	}
}
