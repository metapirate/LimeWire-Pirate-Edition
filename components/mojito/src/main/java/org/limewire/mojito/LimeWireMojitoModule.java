package org.limewire.mojito;

import org.limewire.mojito.util.ContactUtils;

import com.google.inject.AbstractModule;

public class LimeWireMojitoModule extends AbstractModule {

    @Override
    protected void configure() {
        requestStaticInjection(ContactUtils.class);
    }
    
}
