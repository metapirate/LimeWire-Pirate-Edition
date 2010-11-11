package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
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
import org.limewire.nio.observer.Shutdownable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * Default implementation of {@link AltLocFinder}, uses the DHT to find
 * alternate locations.
 */
@Singleton
class AltLocFinderImpl implements AltLocFinder {

    private static final Log LOG = LogFactory.getLog(AltLocFinderImpl.class);
    
    private final DHTManager dhtManager;

    private final AlternateLocationFactory alternateLocationFactory;

    private final AltLocManager altLocManager;

    private final PushEndpointService pushEndpointManager;
    
    @Inject
    public AltLocFinderImpl(DHTManager dhtManager, AlternateLocationFactory alternateLocationFactory, 
            AltLocManager altLocManager, 
            @Named("pushEndpointManager") PushEndpointService pushEndpointManager) {
        this.dhtManager = dhtManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.altLocManager = altLocManager;
        this.pushEndpointManager = pushEndpointManager;
    }
    
    public Shutdownable findAltLocs(URN urn, SearchListener<AlternateLocation> listener) {
        listener = SearchListenerAdapter.nonNullListener(listener);
        
        KUID key = KUIDUtils.toKUID(urn);
        EntityKey lookupKey = EntityKey.createEntityKey(key, AbstractAltLocValue.ALT_LOC);
      
          
          final DHTFuture<FindValueResult> future = dhtManager.get(lookupKey);
          if(future == null) {
              return null;
          } else {
              future.addFutureListener(new AltLocsHandler(dhtManager, urn, key, listener));
              return new Shutdownable() {
                  public void shutdown() {
                      future.cancel(true);
                  }
              };
          }              
    }
    
    /**
     * Looks up the push endpoint for an alternate location based on the guid.
     */
    private void findPushAltLocs(final GUID guid, final URN urn, 
            final DHTValueEntity altLocEntity, final SearchListener<AlternateLocation> listener) {
        
        SearchListener<PushEndpoint> pushEndpointListener = new SearchListener<PushEndpoint>() {
            public void handleResult(PushEndpoint pushEndpoint) {
                // So we made a lookup for AltLocs, the found AltLoc was 
                // firewalled and we made a lookup for its PushProxies.
                // In any case the creator of both values should be the 
                // same Node!
                InetAddress creatorAddress = ((InetSocketAddress)altLocEntity.getCreator().getContactAddress()).getAddress();
                InetAddress externalAddress = pushEndpoint.getInetAddress();
                // external address can be null if not retrieved from DHT
                if (externalAddress != null && !externalAddress.equals(creatorAddress)) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Creator of " + altLocEntity + " and found " + pushEndpoint + " do not match!");
                    }
                    listener.searchFailed();
                } else {
                    AlternateLocation alternateLocation = alternateLocationFactory.createPushAltLoc(pushEndpoint, urn);
                    altLocManager.add(alternateLocation, this);
                    listener.handleResult(alternateLocation);
                }
            }
            
            public void searchFailed() {
                listener.searchFailed();
            }  
            
        };
        pushEndpointManager.findPushEndpoint(guid, pushEndpointListener);
    }
    
    /**
     * The AltLocsHandler listens for the FindValueResult, constructs 
     * AlternateLocations from the results and passes them to AltLocManager 
     * which in turn notifies every Downloader about the new locations.
     */
    private class AltLocsHandler extends AbstractResultHandler {
        
        private final SearchListener<AlternateLocation> listener;
        private final URN urn;

        private AltLocsHandler(DHTManager dhtManager, URN urn, KUID key, 
                SearchListener<AlternateLocation> listener) {
            super(dhtManager, key, listener, AbstractAltLocValue.ALT_LOC);
            this.urn = urn;
            this.listener = listener;
        }
        
        @Override
        protected Result handleDHTValueEntity(DHTValueEntity entity) {
            DHTValue value = entity.getValue();
            if (!(value instanceof AltLocValue)) {
                return Result.NOT_FOUND;
            }
            
            AltLocValue altLoc = (AltLocValue)value;
            
            // If the AltLoc is firewalled then do a lookup for
            // its PushProxies
            if (altLoc.isFirewalled()) {
                if (DHTSettings.ENABLE_PUSH_PROXY_QUERIES.getValue()) {
                    GUID guid = new GUID(altLoc.getGUID());
                    findPushAltLocs(guid, urn, entity, listener);
                    return Result.NOT_YET_FOUND;
                }
            // If it's not then create just an AlternateLocation
            // from the info
            } else {
                Contact creator = entity.getCreator();
                InetAddress addr = ((InetSocketAddress)
                        creator.getContactAddress()).getAddress();
                
                IpPort ipp = new IpPortImpl(addr, altLoc.getPort());
                Connectable c = new ConnectableImpl(ipp, altLoc.supportsTLS());

                long fileSize = altLoc.getFileSize();
                byte[] ttroot = altLoc.getRootHash();
                try {
                    AlternateLocation location = alternateLocationFactory
                            .createDirectDHTAltLoc(c, urn, fileSize, ttroot);
                    altLocManager.add(location, this);
                    listener.handleResult(location);
                    return Result.FOUND;
                } catch (IOException e) {
                    // Thrown if IpPort is an invalid address
                    LOG.error("IOException", e);
                }
            }
            return Result.NOT_FOUND;
        }
    }
    
}
