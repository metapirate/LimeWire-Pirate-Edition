package org.limewire.ui.swing.util;

import java.awt.FileDialog;
import java.awt.Frame;

/**
 * A collection of OSX GUI utilities.
 * <p>
 * This is in a separate class so that we won't have classloading errors if
 * OSX jars aren't included with other installations.
 */
public final class MacUtils {
    
    private MacUtils() {}
    
    /**
     * Returns the OSX Folder Dialog.
     */
    public static FileDialog getFolderDialog(Frame frame) {
        // net.roydesign.ui.FolderDialog:
        // This class takes advantage of a little know trick in 
        // Apple's VMs to show a real folder dialog, with a 
        // Choose button and all.
        return new OSXFolderDialog(frame, ""); 
    }
}
