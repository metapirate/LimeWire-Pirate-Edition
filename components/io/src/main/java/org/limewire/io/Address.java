package org.limewire.io;

/**
 * Represents the network address of a limewire instance.  An <code>Address</code>
 * is a collection of information that enables one limewire instance to make a 
 * network connection to another.  It is an address in the abstract sense, and doesn't
 * necessarily mean an ip and port, although it could include that information.
 * It could also include, for example, a client guid and push proxies.
 * It could also be a jabber id, if jabber messages are used as a signaling
 * channel, for example, to execute reverse connections or firewall transfers.
 * 
 * Addresses should provide meaningful {@link #equals(Object)} and
 * {@link #hashCode()} implementations.
 */
public interface Address {
    
    String getAddressDescription();
    
    public boolean equals(Object obj);
    
    public int hashCode();
}
