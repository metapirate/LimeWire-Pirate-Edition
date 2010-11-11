package org.limewire.ui.swing.util;

/**
 * LogUtils contains static utility methods dealing with Log4J.
 */
public final class LogUtils {

    /**
     * Private constructor prevents unintended instantiation.
     */
    private LogUtils() {
    }

    /**
     * Returns true if the Log4J library is available.
     */
    public static boolean isLog4JAvailable() {
        try {
            Class.forName("org.apache.log4j.LogManager");
            return true;
        } catch (ClassNotFoundException ignore) {
        } catch (NoClassDefFoundError ignore) {
        }
        
        return false;
    }
}
