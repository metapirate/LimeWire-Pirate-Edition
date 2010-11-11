package org.limewire.core.impl.monitor;

import org.limewire.core.api.monitor.IncomingSearchManager;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure the Incoming Search API for the live core. 
 */
public class CoreGlueMonitorModule extends AbstractModule {

    /**
     * Configures the Incoming Search API for the live core. 
     */
    @Override
    protected void configure() {
        bind(IncomingSearchManager.class).to(CoreIncomingSearchManager.class);
    }

}
