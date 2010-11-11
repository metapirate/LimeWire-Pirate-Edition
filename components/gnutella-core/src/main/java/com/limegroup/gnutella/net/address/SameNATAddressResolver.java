package com.limegroup.gnutella.net.address;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.AddressResolver;
import org.limewire.net.address.FirewalledAddress;

import com.google.inject.Inject;
import com.limegroup.gnutella.NetworkManager;

/**
 * Detects if a firewalled address is behind the same NAT and on the same
 * local network as this client and resolves to the local address. Otherwise marks the 
 * firewalled address as resolved.
 */
@EagerSingleton
public class SameNATAddressResolver implements AddressResolver, RegisteringEventListener<AddressEvent> {

    private final static Log LOG = LogFactory.getLog(SameNATAddressResolver.class, LOGGING_CATEGORY);
    
    private final NetworkManager networkManager;

    private final EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster;
    
    /**
     * Ensures that {@link ConnectivityChangeEvent} is only thrown once.
     */
    private final AtomicBoolean localAddressEventGuard = new AtomicBoolean(false);

    @Inject
    public SameNATAddressResolver(NetworkManager networkManager, EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster) {
        this.networkManager = networkManager;
        this.connectivityEventBroadcaster = connectivityEventBroadcaster;
    }
    
    @Inject
    public void register(ListenerSupport<AddressEvent> addressEventListenerSupport) {
        addressEventListenerSupport.addListener(this);
    }
    
    @Inject
    public void register(SocketsManager socketsManager) {
        socketsManager.registerResolver(this);
    }

    public void handleEvent(AddressEvent event) {
        if (areLocalAddressesKnown()) {
            if (localAddressEventGuard.compareAndSet(false, true)) {
                connectivityEventBroadcaster.broadcast(new ConnectivityChangeEvent());
            }
        }
    }

    /**
     * Returns true if the local private and public addresses are known,
     * and the address is a {@link FirewalledAddress} and it is behind the
     * same NAT. 
     */
    @Override
    public boolean canResolve(Address address) {
        if (address instanceof FirewalledAddress) {
            if (areLocalAddressesKnown()) {
                return isBehindThisNAT((FirewalledAddress)address);
            } else {
                LOG.debugf("can not resolve remote address {0} because local address is not known", address);
                return false;
            }
        }
        LOG.debugf("can not resolve remote address {0}", address);
        return false;
    }

    private boolean areLocalAddressesKnown() {
        return NetworkUtils.isValidAddress(networkManager.getExternalAddress())
        && NetworkUtils.isValidAddress(networkManager.getNonForcedAddress());
    }
    
    private boolean isBehindThisNAT(FirewalledAddress address) {
        byte[] thisPublicAddress = networkManager.getExternalAddress();
        if (!Arrays.equals(address.getPublicAddress().getInetAddress().getAddress(), thisPublicAddress)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("different public address: local = {0}, remote = {1}",
                        NetworkUtils.ip2string(thisPublicAddress), address.getPublicAddress());
            }
            return false;
        }
        byte[] thisPrivateAddress = networkManager.getNonForcedAddress();
        if (!NetworkUtils.areInSameSiteLocalNetwork(address.getPrivateAddress().getInetAddress().getAddress(), thisPrivateAddress)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("different site local networks: local = {0}, remote = {1}", NetworkUtils.ip2string(thisPrivateAddress),
                        address.getPrivateAddress());
            }
            return false;
        }
        LOG.debug("addresses behind same NAT!");
        return true;
    }

    /**
     * Resolves a {@link FirewalledAddress} to the {@link Connectable} of its 
     * {@link FirewalledAddress#getPrivateAddress() private address} if this peer
     * and the peer the address belongs to are behind the same firewall.
     * <p>
     * Otherwise resolves the address to a {@link ResolvedFirewalledAddress} to
     * mark it as resolved.
     */
    @Override
    public <T extends AddressResolutionObserver> T resolve(Address addr, T observer) {
        FirewalledAddress address = (FirewalledAddress)addr;
        assert isBehindThisNAT(address) : "not behind same NAT: " + address;
        LOG.debugf("resolved remote address {0} to {1}", address, address.getPrivateAddress());
        observer.resolved(address.getPrivateAddress());
        return observer;
    }

}
