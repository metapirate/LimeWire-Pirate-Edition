package org.limewire.core.impl.search.browse;

import org.limewire.core.api.search.browse.BrowseSearchFactory;

import com.google.inject.AbstractModule;

public class CoreGlueBrowseSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(BrowseSearchFactory.class).to(CoreBrowseSearchFactory.class);
    }

}
