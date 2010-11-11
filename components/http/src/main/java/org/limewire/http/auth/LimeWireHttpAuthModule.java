package org.limewire.http.auth;

import org.limewire.inject.AbstractModule;

public class LimeWireHttpAuthModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(AuthenticationInterceptor.class).to(AuthenticationInterceptorImpl.class);
        bind(AuthenticatorRegistry.class).to(AuthenticatorRegistryImpl.class);
        bind(Authenticator.class).to(AuthenticatorRegistryImpl.class);
    }
}
