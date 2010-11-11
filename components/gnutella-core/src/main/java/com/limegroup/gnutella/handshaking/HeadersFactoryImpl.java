package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HeadersFactoryImpl implements HeadersFactory {
    
    private final HandshakeServices handshakeServices;
    
    @Inject
    public HeadersFactoryImpl(HandshakeServices handshakeServices) {
        this.handshakeServices = handshakeServices;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.handshaking.HeadersFactory#createLeafHeaders(java.lang.String)
     */
    public Properties createLeafHeaders(String remoteIP) {
        return new LeafHeaders(remoteIP, handshakeServices.getLocalIpPort());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.handshaking.HeadersFactory#createUltrapeerHeaders(java.lang.String)
     */
    public Properties createUltrapeerHeaders(String remoteIP) {
        return new UltrapeerHeaders(remoteIP, handshakeServices.getLocalIpPort());
    }

}
