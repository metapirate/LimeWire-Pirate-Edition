package com.limegroup.gnutella;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.messages.BadPacketException;

public interface PushEndpointFactory {

    /** Gets the endpoint for the self. */
    public PushEndpoint createForSelf();
    
    /**
     * creates a PushEndpoint without any proxies.  
     * not very useful but can happen.
     */
    public PushEndpoint createPushEndpoint(byte[] guid);

    /**
     * @param guid the client guid  
     * @param proxies the push proxies for that host, can be empty, see {@link IpPort#EMPTY_SET}.
     */
    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies);

    /**
     * @param guid the client guid  
     * @param proxies the push proxies for that host
     */
    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies, byte features, int version);

    /**
     * @param guid the client guid  
     * @param proxies the push proxies for that host
     */
    public PushEndpoint createPushEndpoint(byte[] guid, Set<? extends IpPort> proxies, byte features, int version, IpPort addr);

    /**
     * creates a PushEndpoint from a String passed in http header exchange.
     */
    public PushEndpoint createPushEndpoint(String httpString) throws IOException;

    /**
     * Constructs a PushEndpoint from binary representation and also updates all
     * other cached {@link PushEndpoint} instances that are cached with the
     * set of read proxies.
     */
    public PushEndpoint createFromBytes(DataInputStream dais) throws BadPacketException, IOException;

}