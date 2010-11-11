package com.limegroup.gnutella.dht;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.dht.db.LimeWireDHTDBModule;

public class LimeWireDHTModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireDHTDBModule());
    }

}
