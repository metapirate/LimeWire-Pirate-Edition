package com.limegroup.gnutella.search;

import com.google.inject.AbstractModule;

public class LimeWireSearchModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SearchResultHandler.class).to(SearchResultHandlerImpl.class);
    }
    
}
