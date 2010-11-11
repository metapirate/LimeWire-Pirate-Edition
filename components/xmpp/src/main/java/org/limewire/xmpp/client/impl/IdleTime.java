package org.limewire.xmpp.client.impl;

public interface IdleTime {
    boolean supportsIdleTime();
    
    long getIdleTime();
}
