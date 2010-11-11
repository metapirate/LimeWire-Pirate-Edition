package org.limewire.core.api.library;

import java.io.File;

/**
 * Allows manipulation of the library.
 */
public interface LibraryData {        

    /** Returns true if files from this directory are allowed to be added. */
    boolean isDirectoryAllowed(File folder);

    /** Returns true if the library is allowed to manage programs. */
    boolean isProgramManagingAllowed();

    /** Returns true if this file is potentially manageable. */
    boolean isFileManageable(File f);
    
    /** Returns the number of files in a the share list directly from the raw library db */
    int peekPublicSharedListCount();

}
