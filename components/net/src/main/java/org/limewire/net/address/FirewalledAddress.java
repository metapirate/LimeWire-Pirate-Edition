package org.limewire.net.address;

import java.util.Comparator;
import java.util.Set;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;


/**
 * Provides the data needed to connect to a firewalled peer.
 * Can be compared to other {@link FirewalledAddress firewalled address objects}
 * using {@link #equals(Object)}.
 */
public class FirewalledAddress implements Address {
    
    private final Connectable publicAddress;
    private final Connectable privateAddress;
    private final Set<Connectable> pushProxies;
    private final int fwtVersion;
    private final GUID clientGuid;
    private int hashCode;
    
    public FirewalledAddress(Connectable publicAddress, Connectable privateAddress, GUID clientGuid, Set<Connectable> pushProxies, int fwtVersion) {
        this.publicAddress = Objects.nonNull(publicAddress, "publicAddress");
        this.privateAddress = Objects.nonNull(privateAddress, "privateAddress");
        this.clientGuid = clientGuid;
        this.pushProxies = pushProxies;
        this.fwtVersion = fwtVersion;
        if (fwtVersion > 0) {
            if (!NetworkUtils.isValidIpPort(publicAddress)) {
                throw new IllegalArgumentException("inconsistent firewalled address: " + this);
            }
        }
    }
    
    @Override
    public String getAddressDescription() {
        if(NetworkUtils.isValidIpPort(getPublicAddress())) {
            return publicAddress.getAddressDescription();
        } else {
            return privateAddress.getAddressDescription();
        }
    }
    
    /**
     * @return an invalid address if public address it not known
     */
    public Connectable getPublicAddress() {
        return publicAddress;
    }
    
    public Connectable getPrivateAddress() {
        return privateAddress;
    }
    
    /**
     * @return a set of push proxy addresses, which may be empty
     */
    public Set<Connectable> getPushProxies() {
        return pushProxies;
    }
    
    /**
     * Returns the version for reliable udp or 0 if it is not supported.
     * 
     * See {@link RUDPUtils#VERSION}.
     */
    public int getFwtVersion() {
        return fwtVersion;
    }
    
    public GUID getClientGuid() {
        return clientGuid;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this, publicAddress, privateAddress, clientGuid, pushProxies, fwtVersion);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FirewalledAddress)) {
            return false;
        }
        FirewalledAddress other = (FirewalledAddress)obj;
        if (!equals(Connectable.COMPARATOR, publicAddress, other.getPublicAddress())) {
            return false;
        }
        if (!equals(Connectable.COMPARATOR, privateAddress, other.getPrivateAddress())) {
            return false;
        }
        if (!Objects.equalOrNull(pushProxies, other.getPushProxies())) {
            return false;
        }
        if (!Objects.equalOrNull(clientGuid, other.getClientGuid())) {
            return false;
        }
        if (fwtVersion != other.getFwtVersion()) {
            return false;
        }
        return true;
    }
    
    private int hashCode(Connectable connectable) {
        if (connectable == null) {
            return 0;
        }
        return connectable.getInetSocketAddress().hashCode() + (connectable.isTLSCapable() ? 1 : 0);
    }
    
    private int hashCode(Set<Connectable> set) {
        int hashCode = 0;
        for (Connectable connectable : set) {
            hashCode = 31 * hashCode + hashCode(connectable);
        }
        return hashCode;
    }
    
    private int hashCode(Object obj) {
        return obj != null ? obj.hashCode() : 0;
    }
    
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int hash = hashCode(publicAddress);
            hash = 31 * hash + hashCode(privateAddress);
            hash = 31 * hash + hashCode(pushProxies);
            hash = 31 * hash + hashCode(clientGuid);
            hash = 31 * hash + fwtVersion;
            hashCode = hash;
        }
        return hashCode;        
    }
    
    private <T> boolean equals(Comparator<T> comparator, T t1, T t2) {
        if (t1 == t2) {
            return true;
        }
        if (t1 == null || t2 == null) {
            return false;
        }
        return comparator.compare(t1, t2) == 0;
    }
}
