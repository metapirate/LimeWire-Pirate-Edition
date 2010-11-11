package org.limewire.rest;

import org.apache.http.nio.protocol.NHttpRequestHandler;

/**
 * Defines a factory for creating REST API request handlers.
 */
public interface RestRequestHandlerFactory {

    /**
     * Creates a request handler for the specified REST target. 
     */
    NHttpRequestHandler createRequestHandler(RestPrefix restTarget);
}
