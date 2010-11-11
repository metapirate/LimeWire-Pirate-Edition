package com.limegroup.gnutella.filters;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.limegroup.gnutella.filters.response.LimeWireFiltersResponseModule;

public class LimeWireFiltersModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireFiltersResponseModule());

        bind(IPFilter.class).to(LocalIPFilter.class);
        bind(IPFilter.class).annotatedWith(Names.named("hostileFilter")).to(HostileFilter.class);
        bind(URNFilter.class).to(URNFilterImpl.class);
        bind(SpamFilterFactory.class).to(SpamFilterFactoryImpl.class);
        bind(URNBlacklistManager.class).to(URNBlacklistManagerImpl.class);
    }
}
