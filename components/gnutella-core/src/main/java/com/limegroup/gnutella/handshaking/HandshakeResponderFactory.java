package com.limegroup.gnutella.handshaking;

public interface HandshakeResponderFactory {

    public HandshakeResponder createUltrapeerHandshakeResponder(
            String host);

    public HandshakeResponder createLeafHandshakeResponder(String host);

}