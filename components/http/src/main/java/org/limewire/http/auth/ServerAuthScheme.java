package org.limewire.http.auth;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.Credentials;

/**
 * Describes a way to do authentication.
 */
public interface ServerAuthScheme {
    /**
     * Sets scheme to be complete which means, authentication successful
     * or no authentication required.
     */
    void setComplete();
    /**
     * @return true if authentication was successful or no authentication is
     * not required
     */
    boolean isComplete();
    /**
     * Parses the credentials from the HTTP request and returns them.
     * @return null if credentials could not be parsed
     */
    Credentials authenticate(HttpRequest request);
    /**
     * Creates the challenge header which is sent to the other side
     * to fulfill the authentication challenge which can be stateful.
     */
    Header createChallenge();
}
