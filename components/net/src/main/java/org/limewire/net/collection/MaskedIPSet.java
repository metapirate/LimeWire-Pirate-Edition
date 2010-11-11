package org.limewire.net.collection;
import java.net.InetAddress;
import java.util.BitSet;

import org.limewire.io.NetworkUtils;

/**
 * A bloom filter for compact memory representation of a set of
 * masked ips.  
 *  /32 (individual ip) = 512 MB
 *  /24 (class C) = 2 MB
 *  /16 (class B) = 64 KB
 */
public class MaskedIPSet extends BitSet {

    private static final long serialVersionUID = 8152241226787435317L;
	private static final int IPV4_SIZE = 32;
    private final int mask;
    
    public MaskedIPSet(int mask) {
        super(assertMask(mask));
        this.mask = IPV4_SIZE - mask;
    }

    private static int assertMask(int size) {
        assert size <= IPV4_SIZE;
        return (0x1 << size);
    }
    
    public void set(InetAddress addr) {
        set(ipV4ToInt(addr));
    }
    
    public void setInt(int ip) {
        set(intToMasked(ip));
    }
    
    public boolean get(InetAddress addr) {
        return get(ipV4ToInt(addr));
    }
    
    public boolean getInt(int ip) {
        return get(intToMasked(ip));
    }
    
    public synchronized boolean getAndSet(InetAddress addr) {
        int index = ipV4ToInt(addr);
        boolean ret = get(index);
        set(index);
        return ret;
    }
    
    private int intToMasked(int ip) {
        int ret = ((ip & (0xFFFFFFFF << mask)) >>> mask);
        assert ret < (0x1 << (32 - mask));
        return ret;
    }
    
    private int ipV4ToInt(InetAddress addr) {
        return intToMasked(NetworkUtils.getMaskedIP(addr, 0xFFFFFFFF)); 
    }
}
