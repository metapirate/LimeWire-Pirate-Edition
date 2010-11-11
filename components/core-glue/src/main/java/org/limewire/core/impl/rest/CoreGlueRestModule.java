package org.limewire.core.impl.rest;

import org.limewire.inject.AbstractModule;
import org.limewire.rest.LimeWireRestModule;

/**
 * Guice module to configure the REST API for the live core.
 */
public class CoreGlueRestModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireRestModule());
        bind(CoreGlueRestService.class);
    }

}
