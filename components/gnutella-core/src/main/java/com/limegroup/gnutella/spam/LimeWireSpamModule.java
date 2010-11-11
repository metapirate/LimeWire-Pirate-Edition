package com.limegroup.gnutella.spam;

import com.google.inject.AbstractModule;

public class LimeWireSpamModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SpamManager.class).to(SpamManagerImpl.class);
    }
}
