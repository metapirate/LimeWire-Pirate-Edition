package org.limewire.core.impl.browse;

import org.limewire.core.api.browse.BrowseFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueBrowseModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(BrowseFactory.class).toProvider(FactoryProvider.newFactory(BrowseFactory.class, CoreBrowse.class));
    }

}
