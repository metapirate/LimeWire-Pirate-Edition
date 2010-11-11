package org.limewire.util;

import java.io.File;

/**
 * Defines the requirements for classes that lock files preventing other 
 * classes from deleting and renaming.
 */
public interface FileLocker {
    
    /** Returns true if the lock was released on the file. */
    public boolean releaseLock(File file);
}
