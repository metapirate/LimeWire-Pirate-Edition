package org.limewire.http.auth;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.nio.protocol.NHttpRequestHandler;

/**
 * Interface to guard handlers requiring authentication.
 * <p>
 * Implementations should intercept {@link HttpRequest http requests} if they match
 * a protected URL, parse out any authentication data and authenticate it, for
 * instance by asking {@link Authenticator}. 
 */
public interface AuthenticationInterceptor extends HttpRequestInterceptor {
    /**
     * Checks if the handler wants to be protected by authentication and
     * returns a guarded handler that wraps <code>handler</code> and only
     * delegates requests to it if authentication was successful.
     * <p>
     * How the {@link AuthenticationInterceptor} decides if an {@link NHttpRequestHandler}
     * wants protection is an implementation detail. The default implementation
     * {@link AuthenticationInterceptorImpl} checks if the handler class is annotated
     * with {@link RequiresAuthentication}.
     */
    NHttpRequestHandler getGuardedHandler(String urlPattern, NHttpRequestHandler handler);
    /**
     * Unregisters a URL pattern.
     * 
     * @param urlPattern might not be registered in the first place
     */
    void unregisterHandler(String urlPattern);
}
