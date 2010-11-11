package org.limewire.core.impl.mojito;

import org.limewire.core.api.mojito.MojitoManager;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure the Mojito API for the live core. 
 */
public class CoreGlueMojitoModule extends AbstractModule {

    /**
     * Configures Mojito API for the live core. 
     */
    @Override
    protected void configure() {
        bind(MojitoManager.class).to(MojitoManagerImpl.class);
    }

}
