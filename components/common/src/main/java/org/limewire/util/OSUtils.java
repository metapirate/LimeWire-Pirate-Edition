package org.limewire.util;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides methods to get operating system properties, resources and versions, 
 * and determine operating system criteria.
 */
public class OSUtils {
    
    private static final Log LOG = LogFactory.getLog(OSUtils.class);
    
    static {
        setOperatingSystems();
    }
    
    /** 
     * Variable for whether or not we're on Windows.
     */
    private static boolean _isWindows;
    
    /** 
     * Variable for whether or not we're on Windows NT.
     */
    private static boolean _isWindowsNT;

    /** 
     * Variable for whether or not we're on Windows XP.
     */
    private static boolean _isWindowsXP;
    
    /** 
     * Variable for whether or not we're on Windows 7.
     */
    private static boolean _isWindows7;

    /** 
     * Variable for whether or not we're on Windows 95.
     */
    private static boolean _isWindows95;

    /** 
     * Variable for whether or not we're on Windows 98.
     */
    private static boolean _isWindows98;

    /** 
     * Variable for whether or not we're on Windows Me.
     */
    private static boolean _isWindowsMe;
       
    /** 
     * Variable for whether or not we're on Windows Vista.
     */
    private static boolean _isWindowsVista;
    
    /**
     * The Windows service pack version - only applicable to XP and Vista.
     */
    private static int _windowsServicePack;

    /** 
     * Variable for whether or not the operating system allows the 
     * application to be reduced to the system tray.
     */
    private static boolean _supportsTray;

    /** 
     * Variable for whether or not we're on Mac OS X.
     */
    private static boolean _isMacOSX;
    
    /** 
     * Variable for whether or not we're on Linux.
     */
    private static boolean _isLinux;

    /** 
     * Variable for whether or not we're on Solaris.
     */
    private static boolean _isSolaris;

    /**
     * Variable for whether or not we're on OS/2.
     */
    private static boolean _isOS2;

    /**
     * Sets the operating system variables.
     */
    public static void setOperatingSystems() {
    	_isWindows = false;
    	_windowsServicePack = 0;
    	_isWindowsVista = false;
    	_isWindowsNT = false;
    	_isWindowsXP = false;
    	_isWindows7 = false;
    	_isWindows95 = false;
    	_isWindows98 = false;
    	_isWindowsMe = false;
    	_isSolaris = false;
    	_isLinux = false;
    	_isOS2 = false;
    	_isMacOSX = false;
    
    	String os = System.getProperty("os.name").toLowerCase(Locale.US);
    	String version = System.getProperty("os.version").toLowerCase(Locale.US);
    	
    	// set the operating system variables
    	_isWindows = os.indexOf("windows") != -1;
    	
    	if (os.indexOf("windows nt") != -1) {
    		_isWindowsNT = true;
    	} else if (os.indexOf("windows xp") != -1) { 
    		_isWindowsXP = true;
    	} else if(os.indexOf("windows 7") != -1) {
            _isWindows7 = true;
        } else if (os.indexOf("windows vista") != -1 && version.startsWith("6.1")) {
            //In jdk 1.6 before update 14 the os.name system property still returns Windows Vista
            //The version number is set to 6.1 however, so we can check for that and windows vista 
            //together to determine if it is windows 7
            _isWindows7 = true;        
        } else if (os.indexOf("windows vista") != -1) {
            _isWindowsVista = true;        
    	} else if(os.indexOf("windows 95") != -1) {
    	   _isWindows95 = true;
    	} else if(os.indexOf("windows 98") != -1) {
    	   _isWindows98 = true;
    	} else if(os.indexOf("windows me") != -1) {
    	   _isWindowsMe = true;
    	} else if(os.indexOf("solaris") != -1) {
    	    _isSolaris = true;    
    	} else if(os.indexOf("linux")   != -1) {
    	    _isLinux   = true;    
    	} else if(os.indexOf("os/2")    != -1) {
    	    _isOS2     = true;    
    	}
        
        if(_isWindows || _isLinux)
            _supportsTray = true;
        
    	if(os.startsWith("mac os")) {
    		if(os.endsWith("x")) {
    			_isMacOSX = true;
    		}
    	}
        
        // If this is Windows XP or Vista, try to find out which service pack
    	// is installed.
    	if(_isWindowsXP || _isWindowsVista) {
    	    try {
    	        String ver = SystemUtils.registryReadText(
    	                "HKEY_LOCAL_MACHINE",
    	                "Software\\Microsoft\\Windows NT\\CurrentVersion",
    	        "CSDVersion");
    	        ver = ver.replaceAll("[^0-9]", "");
    	        _windowsServicePack = Integer.parseInt(ver);
    	        if(LOG.isDebugEnabled())
    	            LOG.debug("Windows service pack " + _windowsServicePack);
    	    } catch(IOException e) {
    	        LOG.debug("Failed to determine Windows service pack", e);
    	    } catch(NumberFormatException e) {
    	        LOG.debug("Failed to determine Windows service pack", e);
    	    }
    	}
    }

    /**
     * Returns the operating system.
     */
    public static String getOS() {
        return System.getProperty("os.name");
    }

    /**
     * Returns the operating system version.
     */
    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    /**
     * Returns the operating system architecture.
     */
    public static String getOSArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Returns true if this is Windows NT or Windows 2000 and
     * hence can support a system tray feature.
     */
    public static boolean supportsTray() {
    	return _supportsTray;
    }

    /**
     * Returns whether or not the OS is some version of Windows.
     *
     * @return <tt>true</tt> if the application is running on some Windows 
     *         version, <tt>false</tt> otherwise
     */
    public static boolean isWindows() {
    	return _isWindows;
    }

    /**
     * Returns whether or not the OS is WinXP.
     *
     * @return <tt>true</tt> if the application is running on WinXP,
     *  <tt>false</tt> otherwise
     */
    public static boolean isWindowsXP() {
    	return _isWindowsXP;
    }
    
    /**
     * Returns whether or not the OS is Windows 7..
     *
     * @return <tt>true</tt> if the application is running on Windows 7,
     *  <tt>false</tt> otherwise
     */
    public static boolean isWindows7() {
        return _isWindows7;
    }
    
    /**
     * @return true if the application is running on Windows NT
     */
    public static boolean isWindowsNT() {
        return _isWindowsNT;
    }
    
    /**
     * @return true if the application is running on Windows 95
     */
    public static boolean isWindows95() {
        return _isWindows95;
    }
    
    /**
     * @return true if the application is running on Windows 98
     */
    public static boolean isWindows98() {
        return _isWindows98;
    }
    
    /**
     * @return true if the application is running on Windows ME
     */
    public static boolean isWindowsMe() {
        return _isWindowsMe;
    }
    
    /**
     * @return true if the application is running on Windows Vista
     */
    public static boolean isWindowsVista() {
        return _isWindowsVista;
    }
    
    /**
     * @return true if the application is running on a windows with
     * the 10 socket limit.
     */
    public static boolean isSocketChallengedWindows() {
        return _isWindowsXP || (_isWindowsVista && _windowsServicePack < 3);
    }
    
    /**
     * Returns true if the application is compatible with the AVG SDK.
     */
    public static boolean isAVGCompatibleWindows() {
        return _isWindows7 || _isWindowsVista || (_isWindowsXP && _windowsServicePack >= 2);
    }

    /**
     * Returns whether or not the OS is OS/2.
     *
     * @return <tt>true</tt> if the application is running on OS/2,
     *         <tt>false</tt> otherwise
     */
    public static boolean isOS2() {
        return _isOS2;
    }

    /** 
     * Returns whether or not the OS is Mac OS X.
     *
     * @return <tt>true</tt> if the application is running on Mac OS X, 
     *         <tt>false</tt> otherwise
     */
    public static boolean isMacOSX() {
    	return _isMacOSX;
    }

    /** 
     * Returns whether or not the OS is Solaris.
     *
     * @return <tt>true</tt> if the application is running on Solaris, 
     *         <tt>false</tt> otherwise
     */
    public static boolean isSolaris() {
    	return _isSolaris;
    }

    /** 
     * Returns whether or not the OS is Linux.
     *
     * @return <tt>true</tt> if the application is running on Linux, 
     *         <tt>false</tt> otherwise
     */
    public static boolean isLinux() {
    	return _isLinux;
    }

    /** 
     * Returns whether or not the OS is some version of
     * Unix, defined here as only Solaris or Linux.
     */
    public static boolean isUnix() {
    	return _isLinux || _isSolaris; 
    }

    /**
     * Returns whether the OS is POSIX-like. 
     */
    public static boolean isPOSIX() {
        return _isLinux || _isSolaris || _isMacOSX;
    }

    /**
     * Returns whether or not this operating system is considered
     * capable of meeting the requirements of a high load server.
     *
     * @return <tt>true</tt> if this OS meets high load server requirements,
     *         <tt>false</tt> otherwise
     */
    public static boolean isHighLoadOS() {
        return !(_isWindows98 || _isWindows95 || _isWindowsMe || _isWindowsNT);
    }

    /**
     * @return true if this is a well-supported version of windows.
     * (not 95, 98, nt or me)
     */
    public static boolean isGoodWindows() {
    	return isWindows() && isHighLoadOS();
    }

    /**
     * Return whether the current operating system supports moving files
     * to the trash. 
     */
    public static boolean supportsTrash() {
        return isWindows() || isMacOSX();
    }
    
    /**
     * Returns the maximum path system of file system of the current OS
     * or a conservative approximation.
     */
    public static int getMaxPathLength() { 
        if (isWindows()) {
            // From Windows NT onwards, Windows applications can create or 
            // manipulate path lengths of 260 characters, where 259 are 
            // actual characters + 1 NULL character. Where path is denoted
            // by the path, filename and extension plus 1 null character.
            //
            // NTFS can actually support 32767 characters in the paths 
            // but one must prepend \\?\ to the file paths. This tells the 
            // APIs to not to enforce 260 character limit.
            return 259;
        }
        else if (isLinux()) {
            return 4096 - 1;
        }
        else {
            return 1024 - 1;
        }
    }
}
