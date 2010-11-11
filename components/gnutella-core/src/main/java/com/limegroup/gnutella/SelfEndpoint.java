package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SelfEndpoint extends AbstractPushEndpoint {
    private final NetworkManager networkManager;
    private final Provider<ConnectionManager> connectionManager;
    private final ApplicationServices applicationServices;
    private final PushEndpointCache pushEndpointCache;
    private final NetworkInstanceUtils networkInstanceUtils;
    
    @Inject
    SelfEndpoint(NetworkManager networkManager,
            ApplicationServices applicationServices,
            Provider<ConnectionManager> connectionManager,
            PushEndpointCache pushEndpointCache,
            NetworkInstanceUtils networkInstanceUtils) {
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.connectionManager = connectionManager;
        this.pushEndpointCache = pushEndpointCache;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /**
     * delegate the call to connection manager
     */
    public Set<? extends IpPort> getProxies() {
        return connectionManager.get().getPushProxies();
    }

    /**
     * we always have the same features
     */
    public byte getFeatures() {
        return 0;
    }

    /**
     * we support the same FWT version if we support FWT at all
     */
    public int getFWTVersion() {
        return networkManager.supportsFWTVersion();
    }

    /**
     * Our address is our external address if it is valid and external.
     * Otherwise we return the BOGUS_IP 
     */
    public String getAddress() {
        byte[] addr = networkManager.getExternalAddress();

        if (NetworkUtils.isValidAddress(addr)
                && !networkInstanceUtils.isPrivateAddress(addr))
            return NetworkUtils.ip2string(addr);

        return RemoteFileDesc.BOGUS_IP;
    }

    /**
     * @return our external address.  First converts it to string since
     * 1.3 jvms does not support getting it from byte[].
     */
    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByName(getAddress());
        } catch (UnknownHostException bad) {
            return null;
        }
    }

    /**
     * Our port is our external port
     */
    public int getPort() {
        if (networkManager.canDoFWT()
                && !networkManager.acceptedIncomingConnection())
            return networkManager.getStableUDPPort();
        return networkManager.getPort();
    }

    public IpPort getValidExternalAddress() {
        try {
            String addr = getAddress();
            int port = getPort();
            if (addr.equals(RemoteFileDesc.BOGUS_IP)
                    || !NetworkUtils.isValidPort(port))
                return null;
            return new IpPortImpl(addr, getPort());

        } catch (UnknownHostException bad) {
            return null;
        }
    }
    
    public boolean isLocal() {
        return true;
    }

    public PushEndpoint createClone() {
        return new PushEndpointImpl(getClientGUID(), getProxies(), getFeatures(),
                getFWTVersion(), getValidExternalAddress(), pushEndpointCache, networkInstanceUtils);
    }

    public byte[] getClientGUID() {
        return applicationServices.getMyGUID();
    }

    public void updateProxies(boolean good) {
    }

    public InetSocketAddress getInetSocketAddress() {
        IpPort externalAddress = getValidExternalAddress();
        if (externalAddress != null) {
            return externalAddress.getInetSocketAddress();
        } else {
            return null;
        }
    }
    
    @Override
    public String getAddressDescription() {
        IpPort addr = getValidExternalAddress();
        return addr == null ? null : addr.getAddress();
    }
    
}