package com.limegroup.gnutella.dht;

import java.util.EventListener;

/**
 * An interface to listen for DHTEvents.
 */
public interface DHTEventListener extends EventListener {
    
    /**
     * Called for DHT events.
     */
    public void handleDHTEvent(DHTEvent evt);
}
