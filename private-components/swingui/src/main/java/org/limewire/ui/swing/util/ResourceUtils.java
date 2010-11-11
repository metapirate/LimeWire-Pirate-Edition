package org.limewire.ui.swing.util;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.OSUtils;

public class ResourceUtils {
    private static final Log LOG = LogFactory.getLog(ResourceUtils.class);

    /**
     * Whether or not the jdic associations were able to load.
     */
    private static boolean LOADED_JDIC_LIBRARY = false;

    static {
        if (OSUtils.isWindows() || OSUtils.isLinux()) {
            try {
                System.loadLibrary("jdic");
                LOADED_JDIC_LIBRARY = true;
            } catch (UnsatisfiedLinkError err) {
                LOG.warn("Error registering JDIC", err);
            }
        }
    }

    /**
     * Determines if the jdic library has loaded.
     */
    public static boolean isJdicLibraryLoaded() {
        return LOADED_JDIC_LIBRARY;
    }

}
