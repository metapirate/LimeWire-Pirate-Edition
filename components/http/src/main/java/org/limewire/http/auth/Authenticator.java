package org.limewire.http.auth;

import org.apache.http.auth.Credentials;

import com.google.inject.Inject;

/**
 * Requirements for an HTTP authenticator that authenticates
 * {@link Credentials}. 
 */
public interface Authenticator {
    /**
     * Call {@link AuthenticatorRegistry#register(Authenticator)} in
     * the implementation and annotate this method with {@link Inject}. 
     */
    void register(AuthenticatorRegistry registry);
    /**
     * Authenticates the given credentials either returning true or false
     * if they can be authenticated.
     */
    boolean authenticate(Credentials credentials);
}
