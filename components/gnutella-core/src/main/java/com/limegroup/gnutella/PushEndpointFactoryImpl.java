package com.limegroup.gnutella;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.messages.BadPacketException;

@Singleton
public class PushEndpointFactoryImpl implements PushEndpointFactory {

    private static final Log LOG = LogFactory.getLog(PushEndpointFactoryImpl.class);
    
    private final Provider<PushEndpointCache> pushEndpointCache;
    private final Provider<SelfEndpoint> selfProvider;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final Provider<IPFilter> hostileFilter;
    
    @Inject
    public PushEndpointFactoryImpl(
            Provider<PushEndpointCache> pushEndpointCache,
            Provider<SelfEndpoint> selfProvider, 
            NetworkInstanceUtils networkInstanceUtils,
            @Named("hostileFilter") Provider<IPFilter> hostileFilter) {
        this.pushEndpointCache = pushEndpointCache;
        this.selfProvider = selfProvider;
        this.networkInstanceUtils = networkInstanceUtils;
        this.hostileFilter = hostileFilter;
    }       
    
    public PushEndpoint createPushEndpoint(byte[] guid) {
        return createPushEndpoint(guid, IpPort.EMPTY_SET);
    }
    
    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies) {
        return createPushEndpoint(guid, proxies, PushEndpoint.PLAIN, 0);
    }
    
    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies, byte features, int version) {
        return createPushEndpoint(guid, proxies, features, version, null);
    }

    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies, byte features, int version, IpPort addr) {
        return new PushEndpointImpl(guid, proxies, features, version, addr, pushEndpointCache.get(), networkInstanceUtils);
    }

    public PushEndpoint createPushEndpoint(String httpString) throws IOException {
        byte[] guid;
        
        if (httpString.length() < 32 ||
                httpString.indexOf(";") > 32)
            throw new IOException("http string does not contain valid guid");
        
        //the first token is the guid
        String guidS=httpString.substring(0,32);
        httpString = httpString.substring(32);
        
        try {
            guid = GUID.fromHexString(guidS);
        } catch(IllegalArgumentException iae) {
            throw new IOException(iae.getMessage());
        }
        
        StringTokenizer tok = new StringTokenizer(httpString,";");        
        Set<IpPort> proxies = new IpPortSet();        
        int fwtVersion =0;        
        IpPort addr = null;
        BitNumbers tlsProxies = null;
        
        while(tok.hasMoreTokens()) {
            String current = tok.nextToken().trim();
            
            // see if this token is the fwt header
            // if this token fails to parse we abort since we must know
            // if the PE supports fwt or not. 
            if (current.startsWith(HTTPConstants.FW_TRANSFER)) {
                fwtVersion = (int) HTTPUtils.parseFeatureToken(current);
                continue;
            }
            
            // don't parse it in the middle of parsing proxies.
            if (proxies.size() == 0 && current.startsWith(PushEndpoint.PPTLS_HTTP)) {
                String value = HTTPUtils.parseValue(current);
                if(value != null) {
                    try {
                        tlsProxies = new BitNumbers(value);
                    } catch(IllegalArgumentException invalid) {
                        throw (IOException)new IOException().initCause(invalid);
                    }
                }
                continue;
            }

            // Only look for more proxies if we didn't reach our limit
            if(proxies.size() < PushEndpoint.MAX_PROXIES) {
                boolean tlsCapable = tlsProxies != null && tlsProxies.isSet(proxies.size());
                // if its not the header, try to parse it as a push proxy
                try {
                    Connectable ipp = NetworkUtils.parseIpPort(current, tlsCapable);
                    if (isGoodPushProxy(ipp)) {
                        proxies.add(ipp);
                    }
                } catch(IOException ohWell) {
                    tlsProxies = null; // stop adding TLS, since our index may be off
                }
            }
            
            // if its not a push proxy, try to parse it as a port:ip
            // only the first occurence of port:ip is parsed
            if (addr==null) {
                try {
                    IpPort ipp = NetworkUtils.parsePortIp(current);
                    if(!networkInstanceUtils.isPrivateAddress(ipp.getInetAddress()))
                        addr = ipp;
                }catch(IOException notBad) {}
            }
            
        }
        
        // if address isn't there or private, reset address and fwt
        if (addr == null || !networkInstanceUtils.isValidExternalIpPort(addr)
                || addr.equals(RemoteFileDesc.BOGUS_IP)) {
            fwtVersion = 0;
            addr = null;
        }
        
        return createPushEndpoint(guid, proxies, (byte)(proxies.size() | fwtVersion << 3), fwtVersion, addr);
    }    

    public PushEndpoint createFromBytes(DataInputStream dais) throws BadPacketException, IOException {
        byte [] guid =new byte[16];
        Set<IpPort> proxies = new IpPortSet(); 
        IpPort addr = null;
        
        byte header = (byte)(dais.read() & 0xFF);
        
        // get the number of push proxies
        byte number = (byte)(header & PushEndpoint.SIZE_MASK); 
        byte features = (byte)(header & PushEndpoint.FEATURES_MASK);
        byte version = (byte)((header & PushEndpoint.FWT_VERSION_MASK) >> 3);
        
        dais.readFully(guid);
        
        if (version > 0) {
            byte [] host = new byte[6];
            dais.readFully(host);
            try {
                addr = NetworkUtils.getIpPort(host, ByteOrder.LITTLE_ENDIAN);
            } catch(InvalidDataException ide) {
                throw new BadPacketException(ide);
            }
            if (addr.getAddress().equals(RemoteFileDesc.BOGUS_IP)) {
                addr = null;
                version = 0;
            }
        }
        
        // If the features mentioned this has pptls bytes, read that.
        BitNumbers bn = null;
        if((features & PushEndpoint.PPTLS_BINARY) != 0) {
            byte[] tlsIndexes = new byte[1];
            dais.readFully(tlsIndexes);
            bn = new BitNumbers(tlsIndexes);
        }   
        
        byte [] tmp = new byte[6];
        for (int i = 0; i < number; i++) {
            dais.readFully(tmp);
            try {
                boolean tlsCapable = bn != null && bn.isSet(i);
                Connectable ipp = NetworkUtils.getConnectable(tmp, ByteOrder.LITTLE_ENDIAN, tlsCapable);
                if (isGoodPushProxy(ipp)) {
                    proxies.add(ipp);
                }
            } catch(InvalidDataException ide) {
                throw new BadPacketException(ide);
            }
        }
        
        /** this adds the read set to the existing proxies */
        PushEndpoint pe = createPushEndpoint(guid, proxies, features, version, addr);
        return pe;
    }    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.PushEndpointFactory#createForSelf()
     */
    public PushEndpoint createForSelf() {
        return selfProvider.get(); 
    }

    boolean isGoodPushProxy(Connectable connectable) {
        if (networkInstanceUtils.isPrivateAddress(connectable.getInetAddress())) {
            LOG.debugf("push proxy not public: {0}", connectable);
            return false;
        }
        if (!hostileFilter.get().allow(connectable)) {
            LOG.debugf("push proxy hostile: {0}", connectable);
            return false;
        }
        return true;
    }
}
