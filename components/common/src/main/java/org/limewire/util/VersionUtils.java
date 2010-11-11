package org.limewire.util;



/**
 * Provides methods to get the current JVM version and compare Java versions.
 */
public class VersionUtils {

    private VersionUtils() {}
    
    /** Utility methods for determining if we're at least Java 1.5. */
    public static boolean isJava15OrAbove() {
        return isJavaVersionOrAbove("1.5");
    }
    
    /** Utility methods for determining if we're at least Java 1.6. */
    public static boolean isJava16OrAbove() {
        return isJavaVersionOrAbove("1.6");
    }
    
    /**
     * Determines if Java is above the given version.
     */
    public static boolean isJavaVersionAbove(String version) {
        try {
            Version java = new Version(getJavaVersion());
            Version given = new Version(version);
            return java.compareTo(given) >= 1;
        } catch(VersionFormatException vfe) {
            return false;
        }
    }
    
    /**
     * Determines if Java is above or equal to the given version.
     */
    public static boolean isJavaVersionOrAbove(String version) {
        try {
            Version java = new Version(getJavaVersion());
            Version given = new Version(version);
            return java.compareTo(given) >= 0;
        } catch(VersionFormatException vfe) {
            return false;
        }
    }

    /**
     * Returns the version of java we're using.
     */
    public static String getJavaVersion() {
    	return System.getProperty("java.version");
    }

}
