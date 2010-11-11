package org.limewire.rest.oauth;

/**
 * Defines an agent used to validate requests using the OAuth protocol.
 */
public interface OAuthValidator {

    /**
     * Validates the specified OAuth request.
     */
    void validateRequest(OAuthRequest request) throws OAuthException;
}
