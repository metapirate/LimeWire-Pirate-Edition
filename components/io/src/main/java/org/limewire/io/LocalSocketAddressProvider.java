package org.limewire.io;

/**
 * Defines the interface for a class to return the local IP address, the local 
 * port and whether the local address is considered private. 
 */
public interface LocalSocketAddressProvider {
    /** Retrieves the current local address. */
    public byte[] getLocalAddress();
    
    /** Retrieves the current local port. */
    public int getLocalPort();
    
    /**
     * Determines whether this provider considers local address
     * (that is, 127.0.0.1, 192.168.*.*, etc...) private addresses.
     */ 
    public boolean isLocalAddressPrivate();
    
    /**
     * Determines if the socket listening on the local addr/port is
     * capable of accepting TLS connections.
     */
    public boolean isTLSCapable();
    
}
