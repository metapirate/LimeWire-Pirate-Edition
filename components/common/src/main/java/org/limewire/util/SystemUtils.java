package org.limewire.util;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Returns system information, where supported, for Windows and OSX. Most methods
 * in <code>SystemUtils</code> rely on native code and fail gracefully if the 
 * native code library isn't found. <code>SystemUtils</code> uses 
 * SystemUtilities.dll for Window environments and libSystemUtilities.jnilib 
 * for OSX.
 */
public class SystemUtils {
    
    private static final Log LOG = LogFactory.getLog(SystemUtils.class);
    
    /**
     * Whether or not the native libraries could be loaded.
     */
    private static final boolean isLoaded;
    
    /**
     * Used by test cases to turn idle time support off.
     */
    private static boolean supportsIdleTime = true;
    
    static {
        boolean canLoad;
        try {
            // Only load the library on systems where we've made it.
            if (OSUtils.isWindows()) {
                if(OSUtils.isGoodWindows()) {
                    System.loadLibrary("SystemUtilities");
                } else {
                    System.loadLibrary("SystemUtilitiesA");
                }
            } else if (OSUtils.isMacOSX()) {
                System.loadLibrary("SystemUtilities");
            }
            
            canLoad = true;
        } catch(UnsatisfiedLinkError noGo) {
            canLoad = false;
        }
        isLoaded = canLoad;
    }
    
    private SystemUtils() {}
    
    
    /**
     * Retrieves the amount of time the system has been idle, where
     * idle means the user has not pressed a key, mouse button, or moved
     * the mouse.  The time returned is in milliseconds.
     */
    public static long getIdleTime() {
    	if(supportsIdleTime()) 
            return idleTime();

        return 0;
    }
    
    /**
     * Returns whether or not the idle time function is supported on this
     * operating system.
     * 
     * @return <tt>true</tt> if we're able to determine the idle time on this
     *  operating system, otherwise <tt>false</tt> if we can't determine, or 
     *  if idle time has been turned off through the setSupportsIdleTime method.
     */
    public static boolean supportsIdleTime() {
        if(supportsIdleTime && isLoaded) {
            if(OSUtils.isGoodWindows())
                return true;
            else if(OSUtils.isMacOSX())
                return true;
        }
            
        return false;
    }
    
    /**
     * Sets the number of open files, if supported.
     */
    public static long setOpenFileLimit(int max) {
        if(isLoaded && OSUtils.isMacOSX())
            return setOpenFileLimit0(max);
        else
            return -1;
    }
    
    /**
     * Sets a file to be writeable.  Package-access so FileUtils can delegate
     * the filename given should ideally be a canonicalized filename.
     */
    static void setWriteable(String fileName) {
        if(isLoaded && (OSUtils.isWindows() || OSUtils.isMacOSX()))
            setFileWriteable(fileName);
    }

    private static native int setOpenFileLimit0(int max);

	/**
	 * Gets the path to the Windows launcher .exe file that is us running right now.
	 * 
	 * @return A String like "c:\Program Files\LimeWire\LimeWire.exe".
	 *         null on error.
	 */
    public static String getRunningPath() {
    	if (OSUtils.isWindows() && isLoaded) {
    		String path = getRunningPathNative();
    		if (path.equals(""))
                return null;
    		else
                return path;
    	}
    	return null;
    }
    
    /** Identifies a special folder in the platform's shell. */
    public static enum SpecialLocations {
        HOME("Home"),
        DOCUMENTS("Documents"),
        APPLICATION_DATA("ApplicationData"),
        DESKTOP("Desktop"),
        START_MENU("StartMenu"),
        START_MENU_PROGRAMS("StartMenuPrograms"),
        START_MENU_STARTUP("StartMenuStartup");
        
        private final String name;
        SpecialLocations(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }

        /**
         * Parse the text of a SpecialLocations identifier into that identifier.
         * 
         * @param name A String name, like "Documents".
         * @return     The matching SpecialLocations identifier, like SpecialLocations.DOCUMENTS.
         *             null if no match.
         */
        public static SpecialLocations parse(String name) {
            for (SpecialLocations location : SpecialLocations.values())
                if (location.getName().equals(name))
                    return location;
            return null;
        }        
    }

    /**
     * Gets the absolute path to a special folder in the platform's shell.
     * 
     * The returned path is specific to the current user, and current to how the user has customized it.
     * Here are the given special folder names and example return paths on Windows XP:
     * 
     * <pre>
     * Home              C:\Documents and Settings\UserName
     * Documents         C:\Documents and Settings\UserName\My Documents
     * Desktop           C:\Documents and Settings\UserName\Desktop
     * ApplicationData   C:\Documents and Settings\UserName\Application Data
     * StartMenu         C:\Documents and Settings\UserName\Start Menu
     * StartMenuPrograms C:\Documents and Settings\UserName\Start Menu\Programs
     * StartMenuStartup  C:\Documents and Settings\UserName\Start Menu\Programs\Startup
     * </pre>
     * 
     * Home, Documents, and Desktop work on Windows, Mac, and Linux, the others are Windows-only.
     * 
     * On Linux, this method sticks "Documents" and "Desktop" on the end of the user's home directory.
     * This will always produce a usable folder.
     * In most distributions, the window manager uses these folders as well.
     * This method does not guarantee Linux has a window manager configured to those folders, however.
     * 
     * @param location A special folder identifier
     * @return         The path to that folder, or null on not supported
     */
    public static String getSpecialPath(SpecialLocations location) {
        if (OSUtils.isWindows()) {
            if (location == SpecialLocations.HOME) {
                return CommonUtils.getUserHomeDir().getPath();
            } else {
                if (isLoaded) {
                    try {
                        String path = getSpecialPathNative(location.getName());
                        if (!path.equals(""))
                            return path;
                        else
                            return null;
                    } catch (UnsatisfiedLinkError error) {
                        LOG.error("Unable to use getSpecialPath!", error); // Old DLL version doesn't have this method
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } else if (OSUtils.isPOSIX()) { // Stick "Documents" and "Desktop" on the user's Mac or Linux home directory
            if (location == SpecialLocations.HOME) {
                return CommonUtils.getUserHomeDir().getPath();
            } else if (location == SpecialLocations.DOCUMENTS) {
                return (new File(CommonUtils.getUserHomeDir(), location.getName())).getPath();
            } else if (location == SpecialLocations.DESKTOP) {
                return (new File(CommonUtils.getUserHomeDir(), location.getName())).getPath();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    /**
     * Changes the icon of a window.
     * Puts the given icon in the title bar, task bar, and Alt+Tab box.
     * Replaces the Swing icon with a real Windows .ico icon that supports multiple sizes, full color, and partially transparent pixels.
     * 
     * @param frame The AWT Component, like a JFrame, that is backed by a native window
     * @param icon  The path to a .exe or .ico file on the disk
     * @return      False on error
     */
    public static boolean setWindowIcon(Component frame, File icon) {
    	if (OSUtils.isWindows() && isLoaded) {
    		String result = setWindowIconNative(frame, System.getProperty("sun.boot.library.path"), icon.getPath());
    	    return result.equals(""); // Returns blank on success, or information about an error
    	}
        
    	return false;
    }
    
    /**
     * Sets a Component to be topmost.
     */
    public static boolean setWindowTopMost(Component frame) {
        if(isLoaded && OSUtils.isWindows()) {
            String result = setWindowTopMostNative(frame, System.getProperty("sun.boot.library.path"));
            return result.equals("");
        }
        
        return false;
    }
    
    /**
     * Flushes the icon cache on the OS, forcing any icons to be redrawn
     * with the current-most icon.
     */
    public static boolean flushIconCache() {
        if(isLoaded && OSUtils.isWindows()) {
            return flushIconCacheNative();
        }
        
        return false;
    }

    /**
     * Reads a numerical value stored in the Windows Registry.
     * 
     * @param root The name of the root registry key, like "HKEY_LOCAL_MACHINE"
     * @param path The path to the registry key with backslashes as separators, like "Software\\Microsoft\\Windows"
     * @param name The name of the variable within that key, or blank to access the key's default value
     * @return     The number value stored there, or 0 on error
     */
    public static int registryReadNumber(String root, String path, String name) throws IOException {
    	if (OSUtils.isWindows() && isLoaded)
    		return registryReadNumberNative(root, path, name);
    	throw new IOException(" not supported ");
    }

    /**
     * Reads a text value stored in the Windows Registry.
     * 
     * @param root The name of the root registry key, like "HKEY_LOCAL_MACHINE"
     * @param path The path to the registry key with backslashes as separators, like "Software\\Microsoft\\Windows"
     * @param name The name of the variable within that key, or blank to access the key's default value
     * @return     The text value stored there or blank on error
     */
    public static String registryReadText(String root, String path, String name) throws IOException {
    	if (OSUtils.isWindows() && isLoaded)
    		return registryReadTextNative(root, path, name);
    	throw new IOException(" not supported ");
    }

    /**
     * Sets a numerical value in the Windows Registry.
     * 
     * @param root  The name of the root registry key, like "HKEY_LOCAL_MACHINE"
     * @param path  The path to the registry key with backslashes as separators, like "Software\\Microsoft\\Windows"
     * @param name  The name of the variable within that key, or blank to access the key's default value
     * @param value The number value to set there
     * @return      False on error
     */
    public static boolean registryWriteNumber(String root, String path, String name, int value) {
    	if (OSUtils.isWindows() && isLoaded)
    		return registryWriteNumberNative(root, path, name, value);
    	else
    		return false;
    }

    /**
     * Sets a text value in the Windows Registry.
     * 
     * @param root  The name of the root registry key, like "HKEY_LOCAL_MACHINE"
     * @param path  The path to the registry key with backslashes as separators, like "Software\\Microsoft\\Windows"
     * @param name  The name of the variable within that key, or blank to access the key's default value
     * @param value The text value to set there
     * @return      False on error
     */
    public static boolean registryWriteText(String root, String path, String name, String value) {
    	if (OSUtils.isWindows() && isLoaded)
    		return registryWriteTextNative(root, path, name, value);
    	else
    		return false;
    }

    /**
     * Deletes a key in the Windows Registry.
     * 
     * @param root The name of the root registry key, like "HKEY_LOCAL_MACHINE"
     * @param path The path to the registry key with backslashes as separators, like "Software\\Microsoft\\Windows"
     * @return     False on error
     */
    public static boolean registryDelete(String root, String path) {
    	if (OSUtils.isWindows() && isLoaded)
    		return registryDeleteNative(root, path);
    	else
    		return false;
    }

    /**
     * Determine if this Windows computer has Windows Firewall on it.
     * 
     * @return True if it does, false if it does not or there was an error
     */
    public static boolean isFirewallPresent() {
    	if (OSUtils.isWindows() && isLoaded)
    		return firewallPresentNative();
    	return false;
    }

    /**
     * Determine if the Windows Firewall is enabled.
     * 
     * @return True if the setting on the "General" tab is "On (recommended)".
     *         False if the setting on the "General" tab is "Off (not recommended)".
     *         False on error.
     */
    public static boolean isFirewallEnabled() {
    	if (OSUtils.isWindows() && isLoaded)
    	    return firewallEnabledNative();
    	return false;
    }

    /**
     * Determine if the Windows Firewall is on with no exceptions.
     * 
     * @return True if the box on the "General" tab "Don't allow exceptions" is checked.
     *         False if the box is not checked.
     *         False on error.
     */
    public static boolean isFirewallExceptionsNotAllowed() {
    	if (OSUtils.isWindows() && isLoaded)
    		return firewallExceptionsNotAllowedNative();
    	return false;
    }

    /**
     * Determine if a program is listed on the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     True if it has a listing on the Exceptions list, false if not or on error
     */
    public static boolean isProgramListedOnFirewall(String path) {
    	if (OSUtils.isWindows() && isLoaded)
    		return firewallIsProgramListedNative(path);
    	return false;
    }

    /**
     * Determine if a program's listing on the Windows Firewall exceptions list has a check box making it enabled.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     True if it's listing's check box is checked, false if not or on error
     */
    public static boolean isProgramEnabledOnFirewall(String path) {
    	if (OSUtils.isWindows() && isLoaded)
    		return firewallIsProgramEnabledNative(path);
    	return false;
    }

    /**
     * Add a program to the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @param name The name of the program, like "LimeWire", this is the text that will identify the item on the list
     * @return     False if error
     */
    public static boolean addProgramToFirewall(String path, String name) {
    	if (OSUtils.isWindows() && isLoaded)
    		return firewallAddNative(path, name);
    	return false;
    }

    /**
     * Remove a program from the Windows Firewall exceptions list.
     * 
     * @param path The path to the program, like "C:\Program Files\LimeWire\LimeWire.exe"
     * @return     False if error.
     */
    public static boolean removeProgramFromFirewall(String path) {
    	if (OSUtils.isWindows() && isLoaded)
    		return firewallRemoveNative(path);
    	return false;
    }

    /**
     * Opens a Web address using the default browser on the native platform.
     * 
     * This method returns immediately, not later after the browser exits.
     * On Windows, this method does the same thing as Start, Run.
     * 
     * @param url The Web address to open
     * @return    0, in place of the process exit code
     */
    public static int openURL(String url) throws IOException {
        if(OSUtils.isWindows() && isLoaded) {
            openURLNative(url);
            return 0; // program's still running, no way of getting an exit code.
        }
        
        throw new IOException("native code not linked");
    }

    /**
     * Runs a path using the default program on the native platform.
     * 
     * Given a path to a program, runs that program.
     * Given a path to a document, opens it in the default program for that kind of document.
     * Given a path to a folder, opens it in the shell.
     * 
     * This method returns immediately, not later after the program exits.
     * On Windows, this method does the same thing as Start, Run.
     * 
     * @param path The complete path to run, like "C:\folder\file.ext"
     * @return     0, in place of the process exit code
     */
    public static int openFile(String path) throws IOException {
        if(OSUtils.isWindows() && isLoaded) {
            openFileNative(path);
            return 0; // program's running, no way to get exit code.
        }
        
        throw new IOException("native code not linked");
    }

    /**
     * Runs a path using the default program on the native platform.
     * 
     * Given a path to a program, runs that program.
     * Given a path to a document, opens it in the default program for that kind of document.
     * Given a path to a folder, opens it in the shell.
     * 
     * Note: this method accepts a parameter list thus should
     *        be generally used with executable files 
     * 
     * This method returns immediately, not later after the program exits.
     * On Windows, this method does the same thing as Start, Run.
     * 
     * @param path The complete path to run, like "C:\folder\file.ext"
     * @param path The list of parameters to pass to the file 
     * @return     0, in place of the process exit code
     */
    public static int openFile(String path, String params) throws IOException {
        if(OSUtils.isWindows() && isLoaded) {
            openFileParamsNative(path, params);
            return 0; // program's running, no way to get exit code.
        }
        
        throw new IOException("native code not linked");
    }
    
    /**
     * Moves a file to the platform-specific trash can or recycle bin.
     * 
     * @param file The file to trash
     * @return     True on success
     */
    public static boolean recycle(File file) {
    	if (OSUtils.isWindows() && isLoaded) {

    		// Get the path to the file
    		String path = null;
			try {
				path = file.getCanonicalPath();
			} catch (IOException err) {
				LOG.error("IOException", err);
				path = file.getAbsolutePath();
			}

			// Use native code to move the file at that path to the recycle bin
			return recycleNative(path);

    	} else {
    		return false;
    	}
    }
    
    /**
     * @return the default String that the shell will execute to open
     * a file with the provided extention.
     * Only supported on windows.
     */
    public static String getDefaultExtentionHandler(String extention) {
    	if (!OSUtils.isWindows() || !isLoaded)
    		return null;

    	if (!extention.startsWith("."))
    		extention = "."+extention;
    	try {
    		String progId = registryReadText("HKEY_CLASSES_ROOT", extention,"");
    		if ("".equals(progId))
    			return "";
    		return registryReadText("HKEY_CLASSES_ROOT",
    				progId+"\\shell\\open\\command","");
    	} catch (IOException iox) {
    		return null;
    	}
    }
    
    /**
     * @return the default String that the shell will execute to open
     * content with the provided mime type.
     * Only supported on windows.
     */
    public static String getDefaultMimeHandler(String mimeType) {
    	if (!OSUtils.isWindows() || !isLoaded)
    		return null;
    	String extention = "";
    	try {
    		extention = registryReadText("HKEY_CLASSES_ROOT", 
    				"MIME\\Database\\Content Type\\"+mimeType, 
    				"Extension");
    	} catch (IOException iox) {
    		return null;
    	}
    	
    	if ("".equals(extention))
    		return "";
    	return getDefaultExtentionHandler(extention);
    }

    /*
     * The following methods are implemented in C++ code in SystemUtilities.dll.
     * In addition, setFileWritable(String) and idleTime() may be implemeted in LimeWire's native library for another platform, like Mac or Linux.
     * The idea is that the Windows, Mac, and Linux libraries have methods with the same names.
     * Call a method, and it will run platform-specific code to complete the task in the appropriate platform-specific way.
     */

    private static native String getRunningPathNative();
    private static native String getSpecialPathNative(String name);
    private static native void openURLNative(String url);
    private static native void openFileNative(String path);
    private static native void openFileParamsNative(String path, String params);
    private static native boolean recycleNative(String path);
    private static native int setFileWriteable(String path);
    private static native long idleTime();
    private static native String setWindowIconNative(Component frame, String bin, String icon);
    private static native String setWindowTopMostNative(Component frame, String bin);
    private static native boolean flushIconCacheNative();

    private static native int registryReadNumberNative(String root, String path, String name) throws IOException ;
    private static native String registryReadTextNative(String root, String path, String name) throws IOException;
    private static native boolean registryWriteNumberNative(String root, String path, String name, int value);
    private static native boolean registryWriteTextNative(String root, String path, String name, String value);
    private static native boolean registryDeleteNative(String root, String path);

    private static native boolean firewallPresentNative();
    private static native boolean firewallEnabledNative();
    private static native boolean firewallExceptionsNotAllowedNative();
    private static native boolean firewallIsProgramListedNative(String path);
    private static native boolean firewallIsProgramEnabledNative(String path);
    private static native boolean firewallAddNative(String path, String name);
    private static native boolean firewallRemoveNative(String path);
}
