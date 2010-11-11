package org.limewire.friend.impl;


import com.google.inject.AbstractModule;

public class LimeWireFriendXmppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendListListeners.class);
    }
}
