package com.limegroup.gnutella;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Set;

import org.limewire.concurrent.ManagedThread;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.api.connection.FirewallTransferStatus;
import org.limewire.core.api.connection.FirewallTransferStatusEvent;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.LimeProps;
import org.limewire.core.settings.SearchSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ProxySettings;
import org.limewire.net.ProxySettings.ProxyType;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.ssl.SSLEngineTest;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.rudp.RUDPUtils;
import org.limewire.service.ErrorService;
import org.limewire.setting.BooleanSetting;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;

@EagerSingleton
public class NetworkManagerImpl implements NetworkManager {

    private static final Log LOG = LogFactory.getLog(NetworkManagerImpl.class);
    
    private final Provider<UDPService> udpService;
    private final Provider<Acceptor> acceptor;
    private final Provider<DHTManager> dhtManager;
    private final Provider<ConnectionManager> connectionManager;
    private final OutOfBandStatistics outOfBandStatistics;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final Provider<CapabilitiesVMFactory> capabilitiesVMFactory;
    private final Provider<ByteBufferCache> bbCache;
    
    private final Object addressLock = new Object();

    private volatile Connectable directAddress;
    private volatile FirewalledAddress firewalledAddress;
    
    
    /** True if TLS is supported for this session. */
    private volatile boolean tlsSupported = true;
    
    private final EventMulticaster<AddressEvent> listeners =
        new CachingEventMulticasterImpl<AddressEvent>(BroadcastPolicy.IF_NOT_EQUALS);
    private final ApplicationServices applicationServices;
    private volatile boolean started;
    
    /**
     * Set of cached proxies, if proxies are known before the external address is.
     */
    private volatile Set<Connectable> cachedProxies;

    private final ProxySettings proxySettings;

    @Inject
    public NetworkManagerImpl(Provider<UDPService> udpService,
            Provider<Acceptor> acceptor,
            Provider<DHTManager> dhtManager,
            Provider<ConnectionManager> connectionManager,
            OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils,
            Provider<CapabilitiesVMFactory> capabilitiesVMFactory,
            Provider<ByteBufferCache> bbCache,
            ApplicationServices applicationServices,
            ProxySettings proxySettings) {
        this.udpService = udpService;
        this.acceptor = acceptor;
        this.dhtManager = dhtManager;
        this.connectionManager = connectionManager;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.bbCache = bbCache;
        this.applicationServices = applicationServices;
        this.proxySettings = proxySettings;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Inject
    void register(ListenerSupport<FirewallStatusEvent> firewallStatusSupport,
                  ListenerSupport<FirewallTransferStatusEvent> firewallTransferStatusSupport) {
        firewallStatusSupport.addListener(new EventListener<FirewallStatusEvent>() {
            @Override
            public void handleEvent(FirewallStatusEvent event) {
                if(started) {
                    // TODO use event
                    maybeFireNewDirectConnectionAddress();
                }
            }
        });    
        firewallTransferStatusSupport.addListener(new EventListener<FirewallTransferStatusEvent>() {
            private volatile FirewallTransferStatus lastStatus = null;
            
            @Override
            public void handleEvent(FirewallTransferStatusEvent event) {
                if(started && lastStatus != event.getData()) {
                    lastStatus = event.getData();
                    updateCapabilities();
                }
            }
        });
    }
    

    public void start() {
        if(isIncomingTLSEnabled() || isOutgoingTLSEnabled()) {
            if(applicationServices.isNewInstall() || applicationServices.isNewJavaVersion() || !SSLSettings.TLS_WORKED_LAST_TIME.getValue()) {
                //block if new install or new java version, or tls did not work last time we ran limewire.
                validateTLS();
            } else {
                new ManagedThread(new Runnable(){
                    @Override
                    public void run() {
                        validateTLS();
                    }
                }, "NetworkManagerImpl.testTLS").start();
            }
        }
        started = true;
    }

    @Override
    public void validateTLS() {
        if(isIncomingTLSEnabled() || isOutgoingTLSEnabled()) {
            SSLEngineTest sslTester = new SSLEngineTest(SSLUtils.getTLSContext(), SSLUtils.getTLSCipherSuites(), bbCache.get());
            if(!sslTester.go()) {
                Throwable t = sslTester.getLastFailureCause();
                setTLSNotSupported(t);
                if(!SSLSettings.IGNORE_SSL_EXCEPTIONS.getValue() && !sslTester.isIgnorable(t))
                    ErrorService.error(t);
            }
            
            SSLSettings.TLS_WORKED_LAST_TIME.setValue(tlsSupported);
        }
    }

    public void stop() {
        started = false;
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(acceptedIncomingConnection());
    }
    
    public void initialize() {
    }
    
    public String getServiceName() {
        return I18nMarker.marktr("Network Management");
    }


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#isIpPortValid()
     */
    public boolean isIpPortValid() {
        return (NetworkUtils.isValidAddress(getAddress()) &&
                NetworkUtils.isValidPort(getPort()));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getUDPConnectBackGUID()
     */
    public GUID getUDPConnectBackGUID() {
        return udpService.get().getConnectBackGUID();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#isOOBCapable()
     */
    public boolean isOOBCapable() {
        if(SearchSettings.FORCE_OOB.getValue())
            return true;
        return isGUESSCapable() && outOfBandStatistics.isSuccessRateGood() &&
               !networkInstanceUtils.isPrivate() &&
               SearchSettings.OOB_ENABLED.getValue() &&
               acceptor.get().isAddressExternal() && isIpPortValid();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#isGUESSCapable()
     */
    public boolean isGUESSCapable() {
    	return udpService.get().isGUESSCapable() && !isProxyEnabled();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getNonForcedPort()
     */
    public int getNonForcedPort() {
        return acceptor.get().getPort(false);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getPort()
     */    
    public int getPort() {
    	return acceptor.get().getPort(true);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getNonForcedAddress()
     */
    public byte[] getNonForcedAddress() {
        return acceptor.get().getAddress(false);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getAddress()
     */
    public byte[] getAddress() {
    	return acceptor.get().getAddress(true);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#getExternalAddress()
     */
    public byte[] getExternalAddress() {
        return acceptor.get().getExternalAddress();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#incomingStatusChanged()
     */
    public boolean incomingStatusChanged() {
        updateCapabilities();
        // Only continue if the current address/port is valid & not private.
        byte addr[] = getAddress();
        int port = getPort();
        if(!NetworkUtils.isValidAddress(addr))
            return false;
        if(networkInstanceUtils.isPrivateAddress(addr))
            return false;            
        if(!NetworkUtils.isValidPort(port))
            return false;
            
        return true;
    }

    private void updateCapabilities() {
        capabilitiesVMFactory.get().updateCapabilities();
        if (connectionManager.get().isShieldedLeaf()) 
            connectionManager.get().sendUpdatedCapabilities();
        synchronized (addressLock) {
            FirewalledAddress address = firewalledAddress;
            if (address != null) {
                newPushProxies(address.getPushProxies());
            }
        }
    }

    public boolean addressChanged() {
        
        // Only continue if the current address/port is valid & not private.
        byte addr[] = getAddress();
        int port = getPort();
        if(!NetworkUtils.isValidAddress(addr))
            return false;
        if(networkInstanceUtils.isPrivateAddress(addr))
            return false;            
        if(!NetworkUtils.isValidPort(port))
            return false;
    
        
        // reset the last connect back time so the next time the TCP/UDP
        // validators run they try to connect back.
        acceptor.get().resetLastConnectBackTime();
        
        // Notify the DHT
        dhtManager.get().addressChanged();
        
    	Properties props = new Properties();
    	props.put(HeaderNames.LISTEN_IP,NetworkUtils.ip2string(addr)+":"+port);
    	HeaderUpdateVendorMessage huvm = new HeaderUpdateVendorMessage(props);
    	
        for(RoutedConnection c : connectionManager.get().getInitializedConnections()) {
    		if (c.getConnectionCapabilities().remoteHostSupportsHeaderUpdate() >= HeaderUpdateVendorMessage.VERSION)
    			c.send(huvm);
    	}
    	
        for(RoutedConnection c : connectionManager.get().getInitializedClientConnections()) {
    		if (c.getConnectionCapabilities().remoteHostSupportsHeaderUpdate() >= HeaderUpdateVendorMessage.VERSION)
    			c.send(huvm);
    	}

        // TODO
        //fireEvent(new AddressEvent(null, Address.EventType.ADDRESS_CHANGED));
        
        return true;
    }

    public void externalAddressChanged() {
        maybeFireNewDirectConnectionAddress();
    }

    private void maybeFireNewDirectConnectionAddress() {
        Connectable newDirectAddress = null;
        boolean fireEvent = false;
        Set<Connectable> proxies = null;
        synchronized (addressLock) {
            if(isDirectConnectionCapable()) {
                newDirectAddress = getPublicAddress(false);
                if (directAddress == null || ConnectableImpl.COMPARATOR.compare(directAddress, newDirectAddress) != 0) {
                    directAddress = newDirectAddress;
                    fireEvent = true;
                    assert NetworkUtils.isValidIpPort(newDirectAddress);
                }
            } else {
                directAddress = null;
                proxies = cachedProxies;
                cachedProxies = null;
            }
        }
        if (fireEvent) {
            fireAddressChange(newDirectAddress);
        } else if (proxies != null) {
            newPushProxies(proxies);
        }
    }

    private boolean isDirectConnectionCapable() {
        return NetworkUtils.isValidAddress(getExternalAddress()) 
        && acceptedIncomingConnection() && NetworkUtils.isValidPort(getPort());
    }

    public void portChanged() {
        maybeFireNewDirectConnectionAddress();
    }

    public void newPushProxies(Set<Connectable> pushProxies) {
        Connectable publicAddress = getPublicAddress(canDoFWT());
        if (!NetworkUtils.isValidIpPort(publicAddress)) {
            cachedProxies = pushProxies;
            return;
        }
        FirewalledAddress newAddress = new FirewalledAddress(publicAddress, getPrivateAddress(), new GUID(applicationServices.getMyGUID()), pushProxies, supportsFWTVersion());
        boolean changed = false;
        synchronized (addressLock) {
            if (!newAddress.equals(firewalledAddress) && directAddress == null) {
                firewalledAddress = newAddress;
                // ensure that we have a valid public address if we support fwts
                assert firewalledAddress.getFwtVersion() == 0 || NetworkUtils.isValidIpPort(firewalledAddress.getPublicAddress());
                changed = true;
            }
        }
        if (changed) {
            fireAddressChange(newAddress);
        }
    }
    
    /**
     * @param udpPort uses stable udp port if true otherwise {@link #getPort()}
     */
    private Connectable getPublicAddress(boolean udpPort) {
        try {
            return new ConnectableImpl(NetworkUtils.ip2string(getExternalAddress()),
                    udpPort ? getStableUDPPort() : getPort(), isIncomingTLSEnabled());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Connectable getPublicAddress() {
        return getPublicAddress(!acceptedIncomingConnection());
    }
    
    private Connectable getPrivateAddress() {
        byte[] privateAddress = getNonForcedAddress();
        try {
            return new ConnectableImpl(new InetSocketAddress(InetAddress.getByAddress(privateAddress), getNonForcedPort()), isIncomingTLSEnabled());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    
    /* (non-Javadoc)
    * @see com.limegroup.gnutella.NetworkManager#acceptedIncomingConnection()
    */
    public boolean acceptedIncomingConnection() {
    	return acceptor.get().acceptedIncoming();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#setListeningPort(int)
     */
    public void setListeningPort(int port) throws IOException {
        acceptor.get().setListeningPort(port);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#canReceiveUnsolicited()
     */
    public boolean canReceiveUnsolicited() {
    	return udpService.get().canReceiveUnsolicited() && !isProxyEnabled();
    }
    
    private boolean isProxyEnabled() {
        return proxySettings.getCurrentProxyType() != ProxyType.NONE;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#canReceiveSolicited()
     */
    public boolean canReceiveSolicited() {
    	return udpService.get().canReceiveSolicited() && !isProxyEnabled();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.NetworkManager#canDoFWT()
     */
    public boolean canDoFWT() {
        return udpService.get().canDoFWT() && !isProxyEnabled();
    }
    
    public int getStableUDPPort() {
        return udpService.get().getStableUDPPort();
    }

    public GUID getSolicitedGUID() {
        return udpService.get().getSolicitedGUID();
    }

    public int supportsFWTVersion() {
        return canDoFWT() ? RUDPUtils.VERSION : 0;
    }
    
    public boolean isPrivateAddress(byte[] addr) {
        return networkInstanceUtils.isPrivateAddress(addr);
    }
    
    /** Disables TLS for this session. */
    private void setTLSNotSupported(Throwable reason) {
        tlsSupported = false;
        if(reason != null) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            reason.printStackTrace(pw);
            pw.flush();
        }
    }
    
    /** Returns true if TLS is disabled for this session. */
    public boolean isTLSSupported() {
        return tlsSupported && !isProxyEnabled();
    }
    
    /** Whether or not incoming TLS is allowed. */
    public boolean isIncomingTLSEnabled() {
        return isTLSSupported() && SSLSettings.TLS_INCOMING.getValue();
    }

    public void setIncomingTLSEnabled(boolean enabled) {
        SSLSettings.TLS_INCOMING.setValue(enabled);
    }

    /** Whether or not outgoing TLS is allowed. */
    public boolean isOutgoingTLSEnabled() {
        return isTLSSupported() && SSLSettings.TLS_OUTGOING.getValue();
    }

    public void setOutgoingTLSEnabled(boolean enabled) {
        SSLSettings.TLS_OUTGOING.setValue(enabled);
    }

    public void addListener(EventListener<AddressEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<AddressEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    private void fireAddressChange(Address newAddress) {
        LOG.debugf("firing new address: {0}", newAddress);
        listeners.broadcast(new AddressEvent(newAddress, AddressEvent.Type.ADDRESS_CHANGED));
    }
    
    private static class SSLSettings extends LimeProps {
    
        private SSLSettings() {}
        
        /**
         * Whether or not TLS worked the last time it was tested.
         */
        public static final BooleanSetting TLS_WORKED_LAST_TIME =
            FACTORY.createBooleanSetting("TLS_WORKED_LAST_TIME", false);
        
        /** Whether or not we want to accept incoming TLS connections. */
        public static final BooleanSetting TLS_INCOMING =
            FACTORY.createBooleanSetting("TLS_INCOMING", true);
        
        /** Whether or not we want to make outgoing connections with TLS. */
        public static final BooleanSetting TLS_OUTGOING =
            FACTORY.createBooleanSetting("TLS_OUTGOING", true);
        
        /** False if we want to report exceptions in TLS handling. */
        public static final BooleanSetting IGNORE_SSL_EXCEPTIONS =
            FACTORY.createRemoteBooleanSetting("IGNORE_SSL_EXCEPTIONS", true);
    
    }

}