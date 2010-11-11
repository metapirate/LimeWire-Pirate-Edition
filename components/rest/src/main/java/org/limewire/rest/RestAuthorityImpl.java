package org.limewire.rest;

import org.apache.http.HttpRequest;
import org.limewire.rest.oauth.OAuthException;
import org.limewire.rest.oauth.OAuthRequest;
import org.limewire.rest.oauth.OAuthValidator;
import org.limewire.rest.oauth.OAuthValidatorFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Implementation of a RestAuthority that uses OAuth to authorize HTTP
 * requests.
 */
class RestAuthorityImpl implements RestAuthority {

    private final OAuthValidator validator;
    
    /**
     * Constructs a RestAuthorityImpl for the specified base URL, port
     * number, and access secret.
     */
    @Inject
    public RestAuthorityImpl(
            @Assisted("baseUrl") String baseUrl,
            @Assisted int port,
            @Assisted("secret") String secret,
            OAuthValidatorFactory validatorFactory) {
        validator = validatorFactory.create(baseUrl, port, secret);
    }
    
    @Override
    public boolean isAuthorized(HttpRequest request) {
        try {
            OAuthRequest oauthRequest = new OAuthRequest(request);
            validator.validateRequest(oauthRequest);
            return true;
        } catch (OAuthException ex) {
            return false;
        }
    }
}
