package org.limewire.core.impl.daap;

import org.limewire.core.api.daap.DaapManager;

import com.google.inject.AbstractModule;

public class CoreGlueDaapModule extends AbstractModule {
        
    @Override
    protected void configure() {
        bind(DaapManager.class).to(DaapManagerImpl.class);
    }
}