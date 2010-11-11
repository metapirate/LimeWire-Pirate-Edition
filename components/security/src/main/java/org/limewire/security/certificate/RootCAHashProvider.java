package org.limewire.security.certificate;


public interface RootCAHashProvider {
    /**
     * @return the current SHA-1 hash encoded to an uppercase hexadecimal string
     *         for the 'official' Lime Wire Root Certificate, or null if the
     *         hash cannot be obtained.
     */
    String getHash();
}
