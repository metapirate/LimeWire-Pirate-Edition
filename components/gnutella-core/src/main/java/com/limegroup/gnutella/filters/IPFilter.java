package com.limegroup.gnutella.filters;

import java.net.SocketAddress;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IP;

/**
 * Defines an interface to find out if an IP address is banned. 
 */

public interface IPFilter extends SpamFilter {
    
    /**
     * @return true if there are black listed hosts in the filter.
     */
    public boolean hasBlacklistedHosts();
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param ip address in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */   
    public boolean allow(IP ip);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param addr an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */
    public boolean allow(SocketAddress addr);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param addr an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */
    public boolean allow(String addr);
    
    /**
     * Checks if the address is of type {@link Connectable} and checks
     * if the IP address is banned.
     * <p>
     * Can be extended to handle other address types if they have the notion
     * of a public/unique IP address.
     * 
     * @return if this host is allowed or the type of address can't be filtered,
     *  false if it is banned or we are unable to create correct IP address out of it.
     */
    public boolean allow(Address address);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param addr an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP address out of it.
     */
    public boolean allow(byte [] addr);
    
    /**
     * Updates the hosts in the IP filter.
     */
    public void refreshHosts();

    /**
     * Updates the hosts in the IP filter and informs the callback, unless the
     * callback is null.
     */
    public void refreshHosts(LoadCallback callback);
}
