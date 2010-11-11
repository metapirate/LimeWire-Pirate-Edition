package org.limewire.core.api.library;

import java.io.IOException;

import org.limewire.core.api.URN;

/**
 * Defines a factory for creating URN values. 
 */
public interface URNFactory {

    /**
     * Creates a new URN from the specified string.  The string should
     * be a SHA1 containing a 32-character value, e.g. 
     * "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB".
     */
    public URN createSHA1Urn(String sha1Urn) throws IOException;
}
