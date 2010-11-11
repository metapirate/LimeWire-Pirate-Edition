package com.limegroup.gnutella.handshaking;

import org.limewire.io.IpPort;

/**
 * Properties for connection handshake, if the node is a client.
 */
public class LeafHeaders extends DefaultHeaders {

    /**
     * Creates a new <tt>LeafHeaders</tt> instance with the specified
     * remote IP.
     *
     * @param remoteIP the IP address of this node as seen by other nodes
     *  on Gnutella -- useful in discovering the real address at the NAT
     *  or firewall
     */
    LeafHeaders(String remoteIP, IpPort localIp){
        super(remoteIP, localIp);
        //set Ultrapeer property
        put(HeaderNames.X_ULTRAPEER, "False");
    }
}
