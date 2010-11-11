package org.limewire.rest;

import org.limewire.rest.oauth.OAuthValidatorFactory;
import org.limewire.rest.oauth.OAuthValidatorImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * Guice module to configure the REST API components.
 */
public class LimeWireRestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RestRequestHandlerFactory.class).to(RestRequestHandlerFactoryImpl.class);
        
        bind(AuthorizationInterceptorFactory.class).toProvider(
                FactoryProvider.newFactory(AuthorizationInterceptorFactory.class, AuthorizationInterceptor.class));
        bind(RestAuthorityFactory.class).toProvider(
                FactoryProvider.newFactory(RestAuthorityFactory.class, RestAuthorityImpl.class));
        bind(OAuthValidatorFactory.class).toProvider(
                FactoryProvider.newFactory(OAuthValidatorFactory.class, OAuthValidatorImpl.class));
    }

}
