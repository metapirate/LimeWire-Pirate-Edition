package org.limewire.rest;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Request interceptor to verify REST authorization.
 */
public class AuthorizationInterceptor implements HttpRequestInterceptor {

    public static final String AUTHORIZED = "authorized";
    public static final String REMOTE_PREFIX = "/remote/";
    
    private final RestAuthority authority;
    
    /**
     * Constructs an AuthorizationInterceptor using the specified authority.
     */
    @Inject
    public AuthorizationInterceptor(@Assisted RestAuthority authority) {
        this.authority = authority; 
    }
    
    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if (request.getRequestLine().getUri().startsWith(REMOTE_PREFIX)) {
            if (authority.isAuthorized(request)) {
                context.setAttribute(AUTHORIZED, Boolean.TRUE);
            } else {
                context.setAttribute(AUTHORIZED, Boolean.FALSE);
            }
        }
    }
}
