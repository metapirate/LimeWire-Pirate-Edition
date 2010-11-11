package com.limegroup.gnutella.handshaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HandshakeResponderFactoryImpl implements HandshakeResponderFactory {

    private final HeadersFactory headersFactory;

    private final HandshakeServices handshakeServices;

    @Inject
    public HandshakeResponderFactoryImpl(HeadersFactory headersFactory,
           HandshakeServices handshakeServices) {
        this.headersFactory = headersFactory;
        this.handshakeServices = handshakeServices;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.handshaking.HandshakeResponderFactory#createUltrapeerHandshakeResponder(java.lang.String)
     */
    public HandshakeResponder createUltrapeerHandshakeResponder(
            String host) {
        return new UltrapeerHandshakeResponder(host, 
                headersFactory, handshakeServices);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.handshaking.HandshakeResponderFactory#createLeafHandshakeResponder(java.lang.String)
     */
    public HandshakeResponder createLeafHandshakeResponder(String host) {
        return new LeafHandshakeResponder(host, headersFactory, handshakeServices);
    }

}
