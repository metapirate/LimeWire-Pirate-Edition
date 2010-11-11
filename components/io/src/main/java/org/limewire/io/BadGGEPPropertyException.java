package org.limewire.io;

/**
 * Thrown when a GGEP extension cannot be found or parsed.
 * Typically other extensions in the block can be extracted.
 */
public class BadGGEPPropertyException extends Exception {
    public BadGGEPPropertyException() {
    }

    public BadGGEPPropertyException(String msg) {
        super(msg);
    }
}
