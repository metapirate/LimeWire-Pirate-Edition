package org.limewire.core.api.library;



/**
 * A File that is displayed in a library.
 */
public interface FileItem extends PropertiableFile {
    
    /** @return the name without the extension */
    String getName(); 
    
    long getSize();

    long getCreationTime();
    
}