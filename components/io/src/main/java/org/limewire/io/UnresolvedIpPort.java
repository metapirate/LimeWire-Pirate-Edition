package org.limewire.io;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * An IpPort that has not been resolved in DNS.
 */
public interface UnresolvedIpPort {
    
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
     * This method can block, looking up a host name in DNS.
     * @return a ResolvedIpPort
     * @throws UnknownHostException if it cannot be resolved
     */
    IpPort resolve() throws UnknownHostException;
    
    public static final List<UnresolvedIpPort> EMPTY_LIST = Collections.emptyList();
}
