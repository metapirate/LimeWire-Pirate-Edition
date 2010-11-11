package org.limewire.http.auth;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.Credentials;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.UriPatternMatcher;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Default implementation of {@link AuthenticationInterceptor}. 
 * <p>
 * Intercepts {@link HttpRequest}, parses out authentication data and puts
 * {@link ServerAuthState} into the {@link HttpContext} so it can be queried
 * by other handlers for authentications state.
 */
@Singleton
public class AuthenticationInterceptorImpl implements AuthenticationInterceptor {
    
    private static final Log LOG = LogFactory.getLog(AuthenticationInterceptorImpl.class);
    
    private final Authenticator authenticator;
    private final UriPatternMatcher protectedURIs;
    
    @Inject
    public AuthenticationInterceptorImpl(Authenticator authenticator) {
        this.authenticator = authenticator;
        protectedURIs = new UriPatternMatcher();
    }

    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        
        ServerAuthState authState = new ServerAuthState();
        ServerAuthScheme authScheme = new BasicServerAuthScheme(authenticator);
        authState.setScheme(authScheme);  // TODO other schemes, scheme registry, etc
        context.setAttribute(ServerAuthState.AUTH_STATE, authState);
        if(protectedURIs.lookup(request.getRequestLine().getUri()) != null) {
            LOG.debugf("entering protected uri: {0}", request.getRequestLine().getUri());
            Credentials credentials = authScheme.authenticate(request);
            if(credentials != null) {
                authState.setCredentials(credentials);
                authScheme.setComplete();
            }                
        } else {
            authScheme.setComplete();
        }
    }
    
    public NHttpRequestHandler getGuardedHandler(String url, NHttpRequestHandler handler) {
        if(isProtected(handler)) {
            protectedURIs.register(url, url);
            return new GuardingHandler(handler);
        } else {
            return handler;
        }
    }
    
    @Override
    public void unregisterHandler(String urlPattern) {
        protectedURIs.unregister(urlPattern);
    }
    
    private boolean isProtected(NHttpRequestHandler handler) {
        return handler.getClass().getAnnotation(RequiresAuthentication.class) != null;        
    }
    
    private static class GuardingHandler implements NHttpRequestHandler {
        private final NHttpRequestHandler handler;

        public GuardingHandler(NHttpRequestHandler handler) {
            this.handler = handler;
        }

        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request, HttpContext context) throws HttpException, IOException {
            ServerAuthState authState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE);
            ServerAuthScheme authScheme = authState.getScheme();
            if(authScheme.isComplete()) {
                return handler.entityRequest(request, context);
            } else {
                return null; // TODO ?    
            }
        }

        public void handle(HttpRequest request, HttpResponse response, NHttpResponseTrigger trigger, HttpContext context) throws HttpException, IOException {
            ServerAuthState authState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE);
            ServerAuthScheme authScheme = authState.getScheme();
            if(authScheme.isComplete()) {
                handler.handle(request, response, trigger, context);
            } else {
                response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
                response.addHeader(authScheme.createChallenge());
                trigger.submitResponse(response);
            }
        }
    }
    
}
