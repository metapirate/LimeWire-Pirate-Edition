package org.limewire.ui.swing.util;

import java.io.File;

import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.SystemUtils.SpecialLocations;


/**
 * A collection of Windows-related GUI utility methods.
 */
public class WindowsUtils {
    
    private WindowsUtils() {}

    /**
     * Determines if we know how to set the login status.
     */    
    public static boolean isLoginStatusAvailable() {
        return OSUtils.isGoodWindows();
    }

    /**
     * Sets the login status.  Only available on W2k+.
     */
    public static void setLoginStatus(boolean allow) {
        if(!isLoginStatusAvailable())
            return;
        
        String path = SystemUtils.getSpecialPath(SpecialLocations.START_MENU_STARTUP);
        
        // Could not get a path for any reason including not loading SystemUtilities.dll
        if (path == null) {
            return;
        }
        
        File startup = new File(path);
        File dst = new File(startup, "LimeWire On Startup.lnk");
        
        // No need to copy if the link is already there
        if (dst.exists())
            return;
        
        File src = new File("LimeWire On Startup.lnk");
        
        if(allow)
            FileUtils.copy(src, dst);
        else
            dst.delete();
    }
}
