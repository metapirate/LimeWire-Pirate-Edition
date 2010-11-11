package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.RUDPMessageFactory;

import com.google.inject.AbstractModule;

public class LimeWireGnutellaRudpMessageModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(RUDPMessageFactory.class).to(LimeRUDPMessageFactory.class);
        bind(LimeRUDPMessageHandler.class);
    }

}
