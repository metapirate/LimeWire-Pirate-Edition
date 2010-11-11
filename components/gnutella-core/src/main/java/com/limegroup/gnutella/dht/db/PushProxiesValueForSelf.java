package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;
import org.limewire.net.address.StrictIpPortSet;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;

/**
 * An implementation of PushProxiesDHTValue for the localhost.
 * 
 * Returns itself as a push proxy if the client is not behind a firewall.
 */
class PushProxiesValueForSelf extends AbstractPushProxiesValue {
    
    private static final long serialVersionUID = -3222117316287224578L;
    private final PushEndpoint self;
    private final NetworkManager networkManager;
    private final ApplicationServices applicationServices;

    public PushProxiesValueForSelf(NetworkManager networkManager,
            PushEndpointFactory pushEndpointFactory,
            ApplicationServices applicationServices) {
        super(AbstractPushProxiesValue.VERSION);
        this.networkManager = networkManager;
        this.self = pushEndpointFactory.createForSelf();
        this.applicationServices = applicationServices;
    }

    @Override
    public DHTValueType getValueType() {
        return AbstractPushProxiesValue.PUSH_PROXIES;
    }

    @Override
    public Version getVersion() {
        return AbstractPushProxiesValue.VERSION;
    }

    public byte[] getValue() {
        return AbstractPushProxiesValue.serialize(this);
    }

    public void write(OutputStream out) throws IOException {
        out.write(getValue());
    }

    public byte[] getGUID() {
        return applicationServices.getMyGUID();
    }
    
    public byte getFeatures() {
        return self.getFeatures();
    }

    public int getFwtVersion() {
        return self.getFWTVersion();
    }
    
    public int getPort() {
        return self.getPort();
    }

    /**
     * Returns a set of push proxies or the client's ip and port if it is not 
     * firewalled. 
     */
    public Set<? extends IpPort> getPushProxies() {
        if (networkManager.acceptedIncomingConnection() && networkManager.isIpPortValid()) {
            // port should be the same as returned in #get
            return new StrictIpPortSet<Connectable>(new ConnectableImpl(new IpPortImpl(networkManager.getAddress(), networkManager.getPort()), networkManager.isIncomingTLSEnabled()));
        } else {
            return self.getProxies();
        }
     }
    
    public BitNumbers getTLSInfo() {
        return AbstractPushProxiesValue.getNumbersFromProxies(getPushProxies());
    }
}