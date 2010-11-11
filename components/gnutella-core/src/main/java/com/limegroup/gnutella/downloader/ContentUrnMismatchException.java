package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Exception thrown when the X-Gnutella-Content-URN does not match the expected
 * sha1 urn.
 * @author Gregorio Roper
 * 
 */
public class ContentUrnMismatchException extends IOException {

    /**
     * Constructor.
     */
    public ContentUrnMismatchException() {
        super("ContentUrnMismatch");
    }
}
