package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

/**
 * An implementation of <code>DHTValue</code> for Gnutella Push Proxies.
 */
public abstract class AbstractPushProxiesValue implements PushProxiesValue {

    /**
     * DHTValueType for Push Proxies.
     */
    public static final DHTValueType PUSH_PROXIES = DHTValueType.valueOf("Gnutella Push Proxy", "PROX");
    
    /**
     * Version of PushProxiesDHTValue.
     */
    public static final Version VERSION = Version.valueOf(0);
        
    static final String CLIENT_ID = "client-id";
    
    static final String FWT_VERSION = "fwt-version";
    
    static final String FEATURES = "features";
    
    static final String PORT = "port";
    
    static final String PROXIES = "proxies";
    
    static final String TLS = "tls";
    
    private final Version version;
    
    public AbstractPushProxiesValue(Version version) {
        this.version = version;
    }

    public DHTValueType getValueType() {
        return PUSH_PROXIES;
    }

    public Version getVersion() {
        return version;
    }

    public int size() {
        return getValue().length;
    }
    
    /**
     * Value based comparison with other object.
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof PushProxiesValue) {
            PushProxiesValue other = (PushProxiesValue)obj;
            return Arrays.equals(getGUID(), other.getGUID()) 
            && getPort() == other.getPort()
            && getFeatures() == other.getFeatures()
            && getFwtVersion() == other.getFwtVersion()
            && getPushProxies().equals(other.getPushProxies())
            && getTLSInfo().equals(other.getTLSInfo());
        }
        return false;
    }
    
    @Override
    public final int hashCode() {
        return getPort() + getFeatures() * 31 + getFwtVersion() * 31 * 31 
        + computeHashCode(getPushProxies()) * 31 * 31 * 31
        + getTLSInfo().hashCode() * 31 * 31 * 31 * 31;
    }
    
    private static final int computeHashCode(Set<? extends IpPort> pushProxies) {
        int hashCode = 1;
        // in case of connectables: we ignore the tls info
        for (IpPort ipPort : pushProxies) {
            hashCode *= 31 * ipPort.getInetSocketAddress().hashCode();
        }
        return hashCode;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("GUID=").append(new GUID(getGUID())).append("\n");
        buffer.append("Features=").append(getFeatures()).append("\n");
        buffer.append("FWTVersion=").append(getFwtVersion()).append("\n");
        buffer.append("PushProxies=").append(getPushProxies()).append("\n");
        return buffer.toString();
    }
    
    /**
     * A helper method to serialize PushProxiesValues
     */
    protected static byte[] serialize(PushProxiesValue value) {
        GGEP ggep = new GGEP();
        ggep.put(CLIENT_ID, value.getGUID());
        // Preserve insertion as an int, not a byte, for backwards compatability.
        ggep.put(FEATURES, (int)value.getFeatures());
        ggep.put(FWT_VERSION, value.getFwtVersion());
        
        byte[] port = new byte[2];
        ByteUtils.short2beb((short)value.getPort(), port, 0);
        ggep.put(PORT, port);
        
        try {
            Set<? extends IpPort> proxies = value.getPushProxies();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (IpPort proxy : proxies) {
                byte[] ipp = NetworkUtils.getBytes(proxy, java.nio.ByteOrder.BIG_ENDIAN);
                assert (ipp.length == 6 || ipp.length == 18);
                baos.write(ipp.length);
                baos.write(ipp);
            }
            baos.close();
            ggep.put(PROXIES, baos.toByteArray());
            
            if (!value.getTLSInfo().isEmpty())
                ggep.put(TLS,value.getTLSInfo().toByteArray());
        } catch (IOException err) {
            // Impossible
            throw new RuntimeException(err);
        }
        
        return ggep.toByteArray();
    }
    
    static BitNumbers getNumbersFromProxies(Set<? extends IpPort> proxies) {
        return BitNumbers.synchronizedBitNumbers(HTTPHeaderUtils.getTLSIndices(proxies));
    }
}
