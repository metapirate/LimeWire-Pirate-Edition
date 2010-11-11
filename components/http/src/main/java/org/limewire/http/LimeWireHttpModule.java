package org.limewire.http;

import org.limewire.http.auth.LimeWireHttpAuthModule;
import org.limewire.http.handler.BasicMimeTypeProvider;
import org.limewire.http.handler.MimeTypeProvider;
import org.limewire.http.httpclient.LimeWireHttpClientModule;
import org.limewire.inject.AbstractModule;

/**
 * <code>Guice</code> module to provide bindings for <code>http</code> component classes.
 */
public class LimeWireHttpModule extends AbstractModule {
    
    @Override
    protected void configure() {
        binder().install(new LimeWireHttpClientModule());
        binder().install(new LimeWireHttpAuthModule());
        bind(MimeTypeProvider.class).to(BasicMimeTypeProvider.class);
    }
}
