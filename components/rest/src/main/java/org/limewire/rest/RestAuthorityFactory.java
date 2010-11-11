package org.limewire.rest;

import com.google.inject.assistedinject.Assisted;

/**
 * Defines a factory for creating instances of RestAuthority.
 */
public interface RestAuthorityFactory {

    /**
     * Creates a RestAuthority with the specified base URL, port number,
     * and access secret.
     */
    RestAuthority create(@Assisted("baseUrl") String baseUrl, int port, 
            @Assisted("secret") String secret);
}
