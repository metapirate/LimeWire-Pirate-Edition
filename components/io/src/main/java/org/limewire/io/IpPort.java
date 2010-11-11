package org.limewire.io;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.limewire.util.ByteUtils;

/**
 * Defines the interface for an object to return an {@link InetAddress}, port 
 * as an integer and the host address as a string.
 */
public interface IpPort {

    /**
     * Assessor for the <tt>InetAddress</tt> for this host.
     * 
     * @return the <tt>InetAddress</tt> for this host
     */
    InetAddress getInetAddress();
    
    /**
     * Assessor for the port this host is listening on.
     * 
     * @return the port this host is listening on
     */
    int getPort();

    /**
     * Assessor for the address string.
     * 
     * @return the address of this host as a string
     */
    String getAddress();
    
    /**
     * Accessor for the InetSocketAddress that can describe the IP & Port.
     * 
     * @return the InetSocketAddress of this host.
     */
    InetSocketAddress getInetSocketAddress();

    
    /** Comparator for IpPort objects that ignores the port. */
    public static final Comparator<IpPort> IP_COMPARATOR = new IpComparator();
        
    /** The sole comparator to use for IpPort objects. */
    public static final Comparator<IpPort> COMPARATOR = new IpPortComparator();
    
    /** An empty list, casted to an IpPort. */
    public static final List<IpPort> EMPTY_LIST = Collections.emptyList();
    /** An empty set, casted to an IpPort. */
    public static final Set<IpPort> EMPTY_SET = Collections.emptySet();
    
    /** An IpPort Comparator that only looks at the IP portion, not the port. */
    public static class IpComparator implements Comparator<IpPort> {
        public int compare(IpPort ip1, IpPort ip2) {
            if(ip1 == ip2)
                return 0;
            byte[] neta = ip1.getInetAddress().getAddress();
            byte[] netb = ip2.getInetAddress().getAddress();
            if(neta[0] == netb[0]) {
                if(neta[1] == netb[1]) {
                    if(neta[2] == netb[2]) {
                        if(neta[3] == netb[3]) {
                            return 0;
                        } else {
                            return compareUnsignedBytes(neta[3], netb[3]);
                        }
                    } else {
                        return compareUnsignedBytes(neta[2], netb[2]);
                    }
                } else {
                    return compareUnsignedBytes(neta[1], netb[1]);
                }
            } else {
                return compareUnsignedBytes(neta[0], netb[0]);
            }
        }
        
        private int compareUnsignedBytes(byte b1, byte b2){
            int i1 = ByteUtils.ubyte2int(b1);
            int i2 = ByteUtils.ubyte2int(b2);
            
            if (i1 == i2){
                return 0;
            }
            
            if (i1 > i2){
                return 1;
            }
            
            return -1;
        }
        
    }
    /**
     * Compares <code>IpPort</code> objects.
     *
     * This class is useful when a variety of objects that implement 
     * <code>IpPort</code> want to be placed in a {@link Set}. Since it is difficult
     * (near impossible) to enforce that they all maintain a valid contract 
     * with regards to hashCode and equals, the only valid way to enforce 
     * <code>Set</code> equality is to use a Comparator that is based on a unique 
     * IP Port criteria.
     */
    public static class IpPortComparator implements Comparator<IpPort> {
        public int compare(IpPort ip1, IpPort ip2) {
            if(ip1 == ip2)
                return 0;
            if (ip1.getPort() != ip2.getPort())
                return ip1.getPort() - ip2.getPort();
            return IP_COMPARATOR.compare(ip1, ip2);
        }
    }    
        
}
