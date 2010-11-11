package org.limewire.io;

import java.net.InetAddress;
import java.net.SocketAddress;


/**
 * A collection of network-related utility methods which are specific
 * to a running instance.
 */
public interface NetworkInstanceUtils {

    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>false</tt>.
     */
    public boolean isMe(String host, int port);

    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  this servent's listening port.
     *
     * @return <tt>true</tt> if the specified host/port combo is this servent,
     *         otherwise <tt>false</tt>.
     */
    public boolean isMe(byte[] address, int port);

    /**
     * Returns true if the given IpPort is the local host
     */
    public boolean isMe(IpPort me);
    

    /**
     * Returns whether or not the two IP addresses share the same
     * first two octets in their address -- the most common
     * indication that they may be on the same network.
     * <p>
     * Private networks are NOT CONSIDERED CLOSE.
     * <p>
     * This method is IPv6 compliant but returns always false if
     * any of the two addresses in a true IPv6 address.
     * 
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    boolean isVeryCloseIP(byte[] addr0, byte[] addr1);
    
    /**
     * Returns whether or not the given IP address shares the same
     * first three octets as the address for this node -- the most 
     * common indication that they may be on the same network.
     *
     * @param addr the address to compare
     */
    boolean isVeryCloseIP(InetAddress addr);
    
    /**
     * Returns whether or not the given IP address shares the same
     * first three octets as the address for this node -- the most 
     * common indication that they may be on the same network.
     *
     * @param addr the address to compare
     */
    boolean isVeryCloseIP(byte[] addr);
    
    /**
     * Returns whether or not this node has a private address.
     *
     * @return <tt>true</tt> if this node has a private address,
     *  otherwise <tt>false</tt>
     */
    boolean isPrivate();
    
    /**
     * Utility method for determining whether or not the given 
     * address is private taking an InetAddress object as argument
     * like the isLocalAddress(InetAddress) method. 
     * <p>
     * This method is IPv6 compliant
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    boolean isPrivateAddress(InetAddress address);
    
    /**
     * Checks if the given address is a private address.
     * <p>
     * This method is IPv6 compliant
     * 
     * @param address the address to check
     */
    boolean isPrivateAddress(byte[] address);
    
    /**
     * Utility method for determining whether or not the given 
     * address is private.  Delegates to 
     * <tt>isPrivateAddress(InetAddress)</tt>.
     * <p>
     * Returns true if the host is unknown.
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    boolean isPrivateAddress(String address);

    /**
     * Utility method for determining whether or not the given 
     * address is private taking an InetAddress object as argument
     * like the isLocalAddress(InetAddress) method. Delegates to 
     * <tt>isPrivateAddress(byte[] address)</tt>.
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    boolean isPrivateAddress(SocketAddress address);

    /**
     * @return whether the IpPort is a valid external address.
     */
    boolean isValidExternalIpPort(IpPort addr);
    
}
