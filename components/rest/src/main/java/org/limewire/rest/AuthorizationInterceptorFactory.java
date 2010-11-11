package org.limewire.rest;

/**
 * Defines a factory for creating an AuthorizationInterceptor.
 */
public interface AuthorizationInterceptorFactory {

    /**
     * Creates an AuthorizationInterceptor using the specified authority.
     */
    AuthorizationInterceptor create(RestAuthority authority);
}
