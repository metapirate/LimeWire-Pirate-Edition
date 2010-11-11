package com.limegroup.gnutella.dht;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * Controls passive leaf nodes (DHT nodes which are Gnutella leaves that 
 * do not fulfill the requirements to become active DHT Nodes). Passive leaf nodes
 * must be able to receive solicited UDP to communicate with DHT nodes. They 
 * also mark mark themselves as firewalled so that nobody will add them to 
 * the RouteTables. 
 * <p>
 * Passive leaf nodes must be connected (via Gnutella) to a DHT 
 * enabled Ultrapeer. The reasoning is that a passive leaf node does not bootstrap, 
 * nor performs any other DHT maintenance operations like refreshing the Buckets 
 * to avoid additional load on the DHT. They depend entirely on their 
 * Ultrapeer which feeds them constantly with fresh Contacts. The RouteTable of 
 * a passive leaf Node is a simple List of size k with LRU eviction (see 
 * <a href="http://en.wikipedia.org/wiki/Cache_algorithms">Least Recently Used</a>
 * caching algorithm). 
 */
class PassiveLeafController extends AbstractDHTController {

    private RouteTable routeTable;
    
    PassiveLeafController(Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher,
            DHTControllerFacade dhtControllerFacade) {
        super(vendor, version, dispatcher, DHTMode.PASSIVE_LEAF,
                dhtControllerFacade);
    }

    @Override
    protected MojitoDHT createMojitoDHT(Vendor vendor, Version version) {
        MojitoDHT dht = MojitoFactory.createFirewalledDHT("PassiveLeafDHT", vendor, version);
        
        ((Context)dht).setBootstrapped(true);
        ((Context)dht).setBucketRefresherDisabled(true);
        
        routeTable = new PassiveLeafRouteTable(vendor, version);
        dht.setRouteTable(routeTable);
        assert (dht.isFirewalled());
        
        return dht;
    }

    @Override
    public void start() {
        super.start();
        
        if (isRunning()) {
            sendUpdatedCapabilities();
        }
    }
}
