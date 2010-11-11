package org.limewire.io;

import com.google.inject.AbstractModule;


public class LimeWireIOModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NetworkInstanceUtils.class).to(NetworkInstanceUtilsImpl.class);
    }

}
