package com.limegroup.gnutella.handshaking;

import java.io.IOException;

import org.limewire.core.settings.ConnectionSettings;

import com.limegroup.gnutella.http.ReadHeadersIOState;

/**
 * Superclass for HandshakeStates that are reading.
 */
abstract class ReadHandshakeState extends ReadHeadersIOState {
    
    protected ReadHandshakeState(HandshakeSupport support) {
        super(support,
              ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue(),
              ConnectionSettings.MAX_HANDSHAKE_LINE_SIZE.getValue());
    }
    
    /** The first state in an incoming handshake. */
    static class ReadRequestState extends ReadHandshakeState {
        ReadRequestState(HandshakeSupport support) {
            super(support);
        }

        /**
         * Ensures the initial connect line is GNUTELLA/0.6
         * or a higher version of the protocol.
         */
        @Override
        protected void processConnectLine() throws IOException {
            if (!((HandshakeSupport)support).notLessThan06(connectLine))
                throw new IOException("not a valid connect string!");
        }

        /** Does nothing. */
        @Override
        protected void processHeaders() throws IOException {}
    }
    
    /** The third state in an incoming handshake, or the second state in an outgoing handshake. */
    static class ReadResponseState extends ReadHandshakeState {
        ReadResponseState(HandshakeSupport support) {
            super(support);
        }
        
        /** Ensures that the connect line began with GNUTELLA/0.6 */
        @Override
        protected void processConnectLine() throws IOException {
            // We do this here, as opposed to in other states, so that
            // our response to the crawler can go through the wire prior
            // to closing the connection.
            // In the case of a crawler, this will normally go:
            // ReadRequestState -> WriteResponseState -> ReadResponseState
            // Normally, ReadResponseState will never get hit because the
            // crawler won't respond & the connection will timeout.
            // However, if it does get hit, we need to close the connection
            if(((HandshakeSupport)support).getReadHandshakeResponse().isCrawler())
                throw new IOException("crawler");
            
            if (!((HandshakeSupport)support).isConnectLineValid(connectLine)) {
                throw new IOException("Bad connect string");
            }
        }

        /** Ensures that the response contained a valid status code. */
        @Override
        protected void processHeaders() throws IOException {
            HandshakeResponse theirResponse = ((HandshakeSupport)support).createRemoteResponse(connectLine);
            switch(theirResponse.getStatusCode()) {
            case HandshakeResponse.OK:
                break;
            default:
                throw NoGnutellaOkException.createServerUnknown(theirResponse.getStatusCode());
            }
        }        
    }    
}
