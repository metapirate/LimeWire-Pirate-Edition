package org.limewire.security.certificate;

/**
 * Generally designed with the idea of doing some sort of lookup for small
 * values (hashes of bigger important values)from a trusted source, initially
 * the DNS system, but in the future other implementations could use straight
 * http or even gnutella. The important part is that the returned results should
 * be TRUSTED, as these values are used to authenticate other important values
 * elsewhere in the system.
 */
public interface HashLookupProvider {
    /**
     * Do a lookup for the given key, using the implementation's backing
     * mechanism.
     * 
     * @return The value for the given key, or null if an error occurs during
     *         lookup
     */
    String lookup(String key);
}
