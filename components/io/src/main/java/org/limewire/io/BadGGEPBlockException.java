package org.limewire.io;

/**
 * Thrown when a GGEP block is hopeless corrupt, making it impossible to extract
 * any of the extensions.  
 */
public class BadGGEPBlockException extends Exception {
    public BadGGEPBlockException() { 
    }

    public BadGGEPBlockException(String msg) { 
        super(msg);
    }
}
