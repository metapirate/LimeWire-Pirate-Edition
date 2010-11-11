package org.limewire.core.impl.support;

import org.limewire.core.api.support.LocalClientInfoFactory;
import org.limewire.core.api.support.SessionInfo;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * Guice module to configure the Support API for the live core. 
 */
public class CoreGlueSupportModule extends AbstractModule {

    /**
     * Configures Support API for the live core. 
     */
    @Override
    protected void configure() {
        bind(SessionInfo.class).to(LimeSessionInfo.class);
        bind(LocalClientInfoFactory.class).toProvider(FactoryProvider.newFactory(
                LocalClientInfoFactory.class, LocalClientInfoImpl.class));
    }

}
