package org.limewire.rest;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.HttpContext;

/**
 * Base class for REST service request handlers.
 */
abstract class AbstractRestRequestHandler implements NHttpRequestHandler {

    /**
     * Default implementation always returns null.
     */
    @Override
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }
    
    @Override
    public final void handle(
            final HttpRequest request,
            final HttpResponse response,
            final NHttpResponseTrigger trigger,
            final HttpContext context) throws HttpException, IOException {
        // Retrieve authorization value from context.
        Object auth = context.getAttribute(AuthorizationInterceptor.AUTHORIZED);
        if ((auth instanceof Boolean) && ((Boolean) auth).booleanValue()) {
            handle(request, response, context);
        } else {
            response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
        }
        
        // Submit response.
        trigger.submitResponse(response);
    }
    
    /**
     * Handles request processing.  This method is called only if the request
     * is properly authorized.
     */
    public abstract void handle(HttpRequest request, HttpResponse response, HttpContext context)
        throws HttpException, IOException;
}
