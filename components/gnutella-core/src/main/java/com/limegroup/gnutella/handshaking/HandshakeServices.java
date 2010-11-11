package com.limegroup.gnutella.handshaking;

import java.util.Collection;
import java.util.List;

import org.limewire.io.IpPort;

/** Defines the interface from which handshaking can get information from other services. */
public interface HandshakeServices {
    
    public IpPort getLocalIpPort();
    
    public List<? extends IpPort> getLeafNodes();
    
    public List<? extends IpPort> getUltrapeerNodes();
    
    public HandshakeStatus getHandshakeStatusForResponse(HandshakeResponse handshakeResponse);
    
    public HandshakeStatus getHandshakeStatusForResponseAsLeaf(HandshakeResponse handshakeResponse);
    
    public boolean isLeafDemotionAllowed();
    
    public boolean isUltrapeerNeeded();
    
    public boolean isUltrapeer();

    /**
     * Returns a collection of IpPorts, preferencing hosts with open slots.
     * If isUltrapeer is true, this preferences hosts with open ultrapeer slots,
     * otherwise it preferences hosts with open leaf slots.
     * <p>
     * Preferences via locale, also.
     * 
     * @param num how many hosts to try to get
     */
    public Collection<IpPort> getAvailableHosts(boolean isUltrapeer,
            String locale, int num);
    
    

}
