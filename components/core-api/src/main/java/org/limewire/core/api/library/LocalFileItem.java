package org.limewire.core.api.library;

import java.io.File;

import org.limewire.friend.api.FileMetaData;

/**
 * A File that is displayed in a library.
 */
public interface LocalFileItem extends FileItem {
    /** Returns the file this is based on. */
    File getFile();

    /** Creates {@link FileMetaData} out of this {@link FileItem}. */
    FileMetaData toMetadata();
    
    /** Determines if this file is sharable. */
    boolean isShareable();
    
    /**True if the file is incomplete.**/
    boolean isIncomplete();
    
    /** Returns the last modified date of the file. */
    long getLastModifiedTime();

    /** Returns the number of times someone has searched for this file. */
    int getNumHits();

    /** Returns the number of uploads this has completed. */
    int getNumUploads();   
    
    /** Returns the number of uploads this has completed. */
    int getNumUploadAttempts();    
    
    /** Returns true if the file has finished loading. Its urn has been calculated. */
    boolean isLoaded();
}
