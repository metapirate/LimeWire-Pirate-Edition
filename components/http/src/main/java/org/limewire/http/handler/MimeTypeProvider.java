package org.limewire.http.handler;

import java.io.File;

/**
 * Defines the requirements for classes that determine the mime type of files.
 */
public interface MimeTypeProvider {

    /**
     * Returns the mime type of <code>file</code>.
     *
     * @return must not return null
     */
    String getMimeType(File file);
    
}
