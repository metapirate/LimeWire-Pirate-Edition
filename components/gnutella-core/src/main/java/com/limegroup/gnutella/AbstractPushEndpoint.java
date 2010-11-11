package com.limegroup.gnutella;

import java.util.Arrays;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

/**
 * Abstract class that does not store any values but provides default implementations
 * for methods that produce a common format.
 */
public abstract class AbstractPushEndpoint implements PushEndpoint {

    public byte[] toBytes(boolean includeTLS) {
        Set<? extends IpPort> proxies = getProxies();
        int payloadSize = getSizeBytes(proxies, includeTLS);
        IpPort addr = getValidExternalAddress();
        int FWTVersion = getFWTVersion();
        if (addr != null && FWTVersion > 0)
            payloadSize+=6;
        byte [] ret = new byte[payloadSize];
        toBytes(ret,0,proxies,addr,FWTVersion, includeTLS);
        return ret;
    }

    public void toBytes(byte[] where, int offset, boolean includeTLS) {
        toBytes(where, offset, getProxies(), getValidExternalAddress(),getFWTVersion(), includeTLS);
    }

    protected void toBytes(byte []where, int offset, Set<? extends IpPort> proxies,
            IpPort address, int FWTVersion, boolean includeTLS) {

        int neededSpace = getSizeBytes(proxies, includeTLS);
        if (address != null) { 
            if (FWTVersion > 0)
                neededSpace+=6;
        } else {
            FWTVersion = 0;
        }
        
        if (where.length-offset < neededSpace)
            throw new IllegalArgumentException ("target array too small");
        
        int featureIdx = offset;
        // store the number of proxies
        where[offset] = (byte)(Math.min(MAX_PROXIES,proxies.size()) 
                | getFeatures() 
                | FWTVersion << 3);
        
        // store the guid
        System.arraycopy(getClientGUID(),0,where,++offset,16);
        offset+=16;
        
        // if we know the external address, store that too
        // if its valid and not private and port is valid
        if (address != null && FWTVersion > 0) {
            byte [] addr = address.getInetAddress().getAddress();
            int port = address.getPort();
            
            System.arraycopy(addr,0,where,offset,4);
            offset+=4;
            ByteUtils.short2leb((short)port,where,offset);
            offset+=2;
        }
        
        // If we're including TLS, then add a byte for which proxies support it.
        
        int pptlsIdx = offset;
        int i=0;
        if(includeTLS) {
            // If any of the proxies are TLS-capable, increment the offset
            for(IpPort ppi : proxies) {
                if(i >= MAX_PROXIES)
                    break;
                
                if(ppi instanceof Connectable && ((Connectable)ppi).isTLSCapable()) {
                    offset++;
                    break;
                }
                
                i++;
            }
        }
        
        // store the push proxies
        i=0;
        for(IpPort ppi : proxies) {
            if(i >= MAX_PROXIES)
                break;
            
            byte [] addr = ppi.getInetAddress().getAddress();
            short port = (short)ppi.getPort();
            
            System.arraycopy(addr,0,where,offset,4);
            offset+=4;
            ByteUtils.short2leb(port,where,offset);
            offset+=2;
            i++;
        }
        
        // insert the tls indices & turn the feature on if TLS should be included
        BitNumbers bn = includeTLS ? HTTPHeaderUtils.getTLSIndices(proxies, (Math.min(proxies.size(), MAX_PROXIES))) : BitNumbers.EMPTY_BN;
        if(!bn.isEmpty()) {
            byte[] tlsIndexes = bn.toByteArray();
            assert tlsIndexes.length == 1;
            where[pptlsIdx] = tlsIndexes[0];
            where[featureIdx] |= PPTLS_BINARY;
        } else {
            where[featureIdx] &= ~PPTLS_BINARY; // make sure its not in the features!
        }
    }
    
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof PushEndpoint) {
            return Arrays.equals(getClientGUID(), ((PushEndpoint)obj).getClientGUID());
        }
        return false;
    }
    
    @Override
    public final int hashCode() {
        return new GUID(getClientGUID()).hashCode();
    }
    
    public String httpStringValue() {
        GUID guid = new GUID(getClientGUID());
        StringBuilder httpString =new StringBuilder(guid.toHexString()).append(";");
        
        //if version is not 0, append it to the http string
        int fwtVersion=getFWTVersion();
        if (fwtVersion!=0) {
            httpString.append(HTTPConstants.FW_TRANSFER)
                .append("/")
                .append(fwtVersion)
                .append(";");
        
            // append the external address of this endpoint if such exists
            // and is valid, non-private and if the port is valid as well.
            IpPort address = getValidExternalAddress();
            if (address!=null) {
                String addr = address.getAddress();
                int port = address.getPort();
                if (!addr.equals(RemoteFileDesc.BOGUS_IP) && 
                        NetworkUtils.isValidPort(port)){
                    httpString.append(port)
                    .append(":")
                    .append(addr)
                    .append(";");
                }
            }
        }
        
        Set<? extends IpPort> proxies = getProxies();
        if (!proxies.isEmpty()) {
            httpString.append(HTTPHeaderUtils.encodePushProxies(proxies, ";", PushEndpoint.MAX_PROXIES));
        } else {
            //trim the ; at the end
            httpString.deleteCharAt(httpString.length()-1);
        }

        return httpString.toString();
    }
    
    @Override
    public String toString() {
        String ret = "PE [FEATURES:"+getFeatures()+",\nFWT Version:"+getFWTVersion()+
        ",\nGUID:"+ new GUID(getClientGUID()) +", address: "+
        getAddress()+":"+getPort()+",\nproxies:{ "; 
        for (IpPort ppi : getProxies()) {
            ret = ret+ppi.getInetAddress()+":"+ppi.getPort()+"\n";
        }
        ret = ret+ "}]";
        return ret;
    }
    
    /**
     * @param proxies the set of proxies for this PE
     * @return how many bytes a PE will use when serialized.
     */
    public static int getSizeBytes(Set<? extends IpPort> proxies, boolean includeTLS) {
        boolean hasTLS = false;
        if(includeTLS) {
            int i = 0;
            for(IpPort ipp : proxies) {
                if(i >= MAX_PROXIES)
                    break;
                
                if(ipp instanceof ConnectableImpl && ((Connectable)ipp).isTLSCapable()) {
                    hasTLS = true;
                    break;
                }
                i++;
            }
        }
        return HEADER_SIZE + (hasTLS ? 1 : 0 ) + Math.min(proxies.size(),MAX_PROXIES) * PROXY_SIZE;
    }

}
