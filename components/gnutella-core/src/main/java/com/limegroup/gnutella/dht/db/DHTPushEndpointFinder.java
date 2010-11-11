package com.limegroup.gnutella.dht.db;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * Searches for {@link PushEndpoint push endpoints} in the DHT.
 */
@Singleton
public class DHTPushEndpointFinder implements PushEndpointService {

    private static final Log LOG = LogFactory.getLog(DHTPushEndpointFinder.class);
    
    private final PushEndpointFactory pushEndpointFactory;
    private final DHTManager dhtManager;

    @Inject
    public DHTPushEndpointFinder(DHTManager dhtManager, PushEndpointFactory pushEndpointFactory) {
        this.dhtManager = dhtManager;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    public void findPushEndpoint(GUID guid, SearchListener<PushEndpoint> listener) {
        listener = SearchListenerAdapter.nonNullListener(listener);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("starting dht lookup for guid: " + guid);
        }
        
        KUID key = KUIDUtils.toKUID(guid);
        EntityKey lookupKey = EntityKey.createEntityKey(key, AbstractPushProxiesValue.PUSH_PROXIES);        
      
        DHTFuture<FindValueResult> future = dhtManager.get(lookupKey);
        if(future != null) {                        
            future.addFutureListener(new PushEndpointHandler(dhtManager, guid, key, listener));            
        } else {
            LOG.debug("dht manager not bootstrapped or no dht");
            listener.searchFailed();
        }               
    }

    public PushEndpoint getPushEndpoint(GUID guid) {
        BlockingSearchListener<PushEndpoint> listener = new BlockingSearchListener<PushEndpoint>();
        findPushEndpoint(guid, listener);
        return listener.getResult();
    }

    /**
     * The PushAltLocsHandler listens for FindValueResults, constructs PushEndpoints
     * from the results and passes them to AltLocManager which in turn notifies all
     * Downloads about the new alternate locations.
     */
    private class PushEndpointHandler extends AbstractResultHandler {
        
        private final GUID guid;
        
        private final SearchListener<PushEndpoint> listener;

        private PushEndpointHandler(DHTManager dhtManager, GUID guid, 
                KUID key, SearchListener<PushEndpoint> listener) {
            super(dhtManager, key, listener, AbstractPushProxiesValue.PUSH_PROXIES);
            this.guid = guid;
            this.listener = listener;
        }
        
        @Override
        protected Result handleDHTValueEntity(DHTValueEntity entity) {

            DHTValue value = entity.getValue();
            if (!(value instanceof PushProxiesValue)) {
                return Result.NOT_FOUND;
            }
            
            Contact creator = entity.getCreator();
            InetAddress addr = ((InetSocketAddress)creator.getContactAddress()).getAddress();
            
            PushProxiesValue pushProxies = (PushProxiesValue)value;
            
            // Compare the GUIDs from the AltLoc and PushProxy value!
            // They should be the same!
            byte[] guid = this.guid.bytes();
            if (!Arrays.equals(guid, pushProxies.getGUID())) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("The AltLoc and PushProxy GUIDs do not match!");
                }
                return Result.NOT_FOUND;
            }
            
            Set<? extends IpPort> proxies = pushProxies.getPushProxies();
            byte features = pushProxies.getFeatures();
            int fwtVersion = pushProxies.getFwtVersion();
            IpPort ipp = new IpPortImpl(addr, pushProxies.getPort());
            PushEndpoint pe = pushEndpointFactory.createPushEndpoint(guid, proxies, features, fwtVersion, ipp);
            if (LOG.isDebugEnabled()) {
                LOG.debug("push endpoint found: " + pe);
            }
            listener.handleResult(pe);
            return Result.FOUND;
        }
    }
}
