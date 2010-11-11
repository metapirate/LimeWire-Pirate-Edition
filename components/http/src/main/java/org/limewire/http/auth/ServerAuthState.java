package org.limewire.http.auth;

import org.apache.http.auth.Credentials;
import org.apache.http.protocol.HttpContext;

/**
 * Context object for HTTP authentication that is attached to
 * {@link HttpContext}.
 */
public class ServerAuthState {

    public static final String AUTH_STATE = "http.server.auth"; 

    private ServerAuthScheme scheme;
    private Credentials credentials;

    /**
     * Returns the used authentication scheme. 
     */
    public ServerAuthScheme getScheme() {
        return scheme;
    }

    /**
     * Sets the used authentication scheme. 
     */
    public void setScheme(ServerAuthScheme scheme) {
        this.scheme = scheme;
    }

    /**
     * Returns the set credentials. 
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Sets the credentials sent in the HTTP request. 
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
}
