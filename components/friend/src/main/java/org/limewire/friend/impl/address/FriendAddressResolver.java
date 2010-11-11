package org.limewire.friend.impl.address;

import java.io.IOException;
import java.net.URI;
import java.util.Map.Entry;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.ConnectBackRequestFeature;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.feature.FeatureEvent.Type;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.AddressResolver;
import org.limewire.net.address.FirewalledAddress;

import com.google.inject.Inject;

/**
 * Resolves addresses of type {@link FriendAddress} by looking up the full jabber id 
 * including resource in the logged in users.
 */
@EagerSingleton
public class FriendAddressResolver implements AddressResolver {

    private final static Log LOG = LogFactory.getLog(FriendAddressResolver.class, LOGGING_CATEGORY);
    
    private final EventBean<FriendConnectionEvent> connectionEventBean;

    private final EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster;
    
    private final ConnectivyFeatureListener connectivyFeatureListener = new ConnectivyFeatureListener();

    private final SocketsManager socketsManager;
    private final FriendAddressRegistry addressRegistry;

    @Inject
    public FriendAddressResolver(EventBean<FriendConnectionEvent> connectionEventBean, EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster,
            SocketsManager socketsManager, FriendAddressRegistry addressRegistry) {
        this.connectionEventBean = connectionEventBean;
        this.connectivityEventBroadcaster = connectivityEventBroadcaster;
        this.socketsManager = socketsManager;
        this.addressRegistry = addressRegistry;
    }

    @Inject void register(SocketsManager socketsManager, ListenerSupport<FeatureEvent> featureSupport) {
        socketsManager.registerResolver(this);
        featureSupport.addListener(connectivyFeatureListener);
    }
    
    @Override
    public boolean canResolve(Address address) {
        if (address instanceof FriendAddress) {
            FriendAddress friendIdAddress = (FriendAddress)address;
            FriendPresence presence = getPresence(friendIdAddress);
            if (presence == null) {
                LOG.debugf("can not resolve remote address {0} because no presence was found}", friendIdAddress);
                return false;
            }
            LOG.debugf("can resolve remote address {0}", address);
            return true;
        }
        LOG.debugf("can not resolve remote address {0}", address);
        return false;
    }
    
    /**
     * Returns the friend presence belonging to an <code>address</code>.
     * 
     * @return null if not presence is found for the address, i.e. the user
     * is not online for example
     */
    public FriendPresence getPresence(FriendAddress address) {
        String id = address.getId();
        FriendConnectionEvent connection = connectionEventBean.getLastEvent();
        if(connection == null || connection.getType() != FriendConnectionEvent.Type.CONNECTED)
            return null;
        Friend friend = connection.getSource().getFriend(id);
        if(friend != null) {
            for(Entry<String, FriendPresence> entry : friend.getPresences().entrySet()) {
                FriendPresence resolvedPresence =
                    getMatchingPresence(address, entry.getKey(), entry.getValue());
                if(resolvedPresence != null) {
                    return resolvedPresence;
                }
            }
        }
        return null;
    }

    /**
     * Returns presence if presence is {@link FriendPresence} and the resource
     * id matches the one in <code>address</code> and the address is available in
     * the presence.
     * <p>
     * Also ensures that auth-token and address feature are set.
     */
    private FriendPresence getMatchingPresence(FriendAddress friendAddress, String resourceId, FriendPresence presence) {
        if (friendAddress.equals(new FriendAddress(resourceId))) {
            // only return address actual address is not null and auth-token is 
            // available too, otherwise the address is worthless still
            Address address = addressRegistry.get(friendAddress);
            Feature authTokenFeature = presence.getFeature(AuthTokenFeature.ID);
            if(address != null && authTokenFeature != null) {
                return presence;
            }
            LOG.debugf("address is {0}, token features is {1}", address, authTokenFeature);
        }
        return null;
    }

    @Override
    public <T extends AddressResolutionObserver> T resolve(Address address, T observer) {
        LOG.debugf("resolving: {0}", address);
        FriendAddress friendAddress = (FriendAddress)address;
        FriendPresence resolvedPresence = getPresence(friendAddress);
        if (resolvedPresence == null) {
            LOG.debugf("{0} could not be resolved", address);
            observer.handleIOException(new IOException("Could not be resolved"));
        } else {
            Address resolvedAddress = addressRegistry.get(friendAddress);
            // race condition, address could have been nulled in the mean time,
            // although it was checked in getPresence() 
            if (resolvedAddress == null) {
                LOG.debugf("could not resolve {0}, not in registry {1}", address, addressRegistry);
                observer.handleIOException(new IOException("could not be resolved, no address"));
            } else if (resolvedAddress instanceof FirewalledAddress) {
                // if it's a firewalled address, see if sockets manager can resolve if further, i.e.
                // if SameNATResolver can take care of it
                if (socketsManager.canResolve(resolvedAddress)) {
                    LOG.debugf("can be same nat resolved {0}", address);
                    socketsManager.resolve(resolvedAddress, observer);
                } else if (resolvedPresence.hasFeatures(ConnectBackRequestFeature.ID)) {
                    // else make it an xmpp firewalled address, so connect requests can be sent over xmpp
                    resolvedAddress = new FriendFirewalledAddress(friendAddress, (FirewalledAddress)resolvedAddress);
                    LOG.debugf("resolved {0} as xmpp firewalled address {1}", address, resolvedAddress);
                    observer.resolved(resolvedAddress);
                } else {
                    LOG.debugf("resolved {0} as firewalled address {1}", address, resolvedAddress);
                    observer.resolved(resolvedAddress);
                }
            } else {
                LOG.debugf("resolved {0} as non-firewalled address {1}", address, resolvedAddress);
                observer.resolved(resolvedAddress);
            }
        }
        return observer;
    }

    private class ConnectivyFeatureListener implements EventListener<FeatureEvent> {
        @Override
        public void handleEvent(FeatureEvent event) {
            if (event.getType() != Type.ADDED) {
                return;
            }
            URI id = event.getData().getID();
            if (id.equals(AuthTokenFeature.ID) || id.equals(AddressFeature.ID)) {
                FriendPresence presence = event.getSource();
                if (presence.hasFeatures(AuthTokenFeature.ID, AddressFeature.ID)) {
                    LOG.debugf("presence with address and auth-token became available: {0}", presence.getPresenceId());
                    connectivityEventBroadcaster.broadcast(new ConnectivityChangeEvent());    
                }
            }
        }
    }
}