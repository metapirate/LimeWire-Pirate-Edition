package org.limewire.rest.oauth;

import com.google.inject.assistedinject.Assisted;

/**
 * Defines a factory for creating instances of OAuthValidator.
 */
public interface OAuthValidatorFactory {

    /**
     * Creates an OAuthValidator with the specified base URL, port number,
     * and access secret.
     */
    OAuthValidator create(@Assisted("baseUrl") String baseUrl, int port, 
            @Assisted("secret") String secret);
}
