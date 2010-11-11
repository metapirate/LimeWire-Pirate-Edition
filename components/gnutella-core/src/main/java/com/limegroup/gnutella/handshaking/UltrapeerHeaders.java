package com.limegroup.gnutella.handshaking;

import org.limewire.io.IpPort;

/**
 * Headers that should be sent only by Ultrapeers.
 */
public class UltrapeerHeaders extends DefaultHeaders {
    
    // we currently support version 0.1 of probes - maybe probes will be folded
    // into dynamic querying so we can get rid of the header???
    public final static String PROBE_VERSION = "0.1";

    /**
     * Creates a new <tt>UltrapeerHeaders</tt> instance with the specified
     * remote IP.
     *
     * @param remoteIP the IP address of this node as seen by other nodes
     *  on Gnutella -- useful in discovering the real address at the NAT
     *  or firewall
     */
    UltrapeerHeaders(String remoteIP, IpPort localIp) {
        super(remoteIP, localIp);
        //set Ultrapeer property
        put(HeaderNames.X_ULTRAPEER, "True");
        put(HeaderNames.X_PROBE_QUERIES, PROBE_VERSION);
    }
    
}

