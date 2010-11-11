package org.limewire.security.certificate;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementations will calculate hashes based on their specific underlying
 * algorithm.
 */
public interface HashCalculator {
    /**
     * Reads the entire stream, closes the stream, and returns the hash value.
     */
    byte[] calculate(InputStream in) throws IOException;

    /**
     * Calculates the hash for the given input array.
     */
    byte[] calculate(byte in[]);
}
