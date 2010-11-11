package org.limewire.rest;

import org.apache.http.HttpRequest;

/**
 * Defines an entity for authorizing REST requests.
 */
public interface RestAuthority {

    /**
     * Returns true if the specified HTTP request is authorized for REST
     * services.
     */
    boolean isAuthorized(HttpRequest request);
}
