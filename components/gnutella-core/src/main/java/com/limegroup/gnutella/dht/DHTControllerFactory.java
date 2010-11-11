package com.limegroup.gnutella.dht;

import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.util.EventDispatcher;

/**
 * Defines the interface to create DHT nodes in various modes 
 * (active, passive or a passive leaf). 
 */
public interface DHTControllerFactory {

    public ActiveDHTNodeController createActiveDHTNodeController(Vendor vendor,
            Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher);

    public PassiveDHTNodeController createPassiveDHTNodeController(
            Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher);

    public PassiveLeafController createPassiveLeafController(Vendor vendor,
            Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher);

}