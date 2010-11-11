package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.http.WriteHeadersIOState;

/** Superclass for HandshakeStates that are written out. */
public abstract class WriteHandshakeState extends WriteHeadersIOState {
    
    /** The handshake support. */
    protected final HandshakeSupport support;
    
    protected WriteHandshakeState(HandshakeSupport support) {
        super();
        this.support = support;
    }
    

    /** The second state in an incoming handshake, or the third state in an outgoing handshake. */
    static class WriteResponseState extends WriteHandshakeState {
        private HandshakeResponder responder;
        private HandshakeResponse response;
        private boolean outgoing;
        
        /**
         * Constructs a new WriteResponseState using the given support, responder,
         * and whether or not we're responding to an outgoing or incoming request.
         * 
         * @param support
         * @param responder
         * @param outgoing
         */
        WriteResponseState(HandshakeSupport support, HandshakeResponder responder, boolean outgoing) {
            super(support);
            this.responder = responder;
            this.outgoing = outgoing;
        }

        /**
         * Creates a response using the responder and wraps it into a ByteBuffer.
         */
        @Override
        protected ByteBuffer createOutgoingData() throws IOException {
            // The distinction between requests is not necessary for correctness,
            // but is useful.  The getReadHandshakeRemoteResponse() method will
            // contain the correct response status code & msg, whereas
            // the getReadHandshakeResponse() method assumes '200 OK'.
            HandshakeResponse theirResponse;
            if(outgoing)
                theirResponse = support.getReadHandshakeRemoteResponse();
            else
                theirResponse = support.getReadHandshakeResponse();
            response = responder.respond(theirResponse, outgoing);
            StringBuilder sb = new StringBuilder();
            support.appendResponse(response, sb);
            return ByteBuffer.wrap(StringUtils.toAsciiBytes(sb.toString()));
        }

        /**
         * Throws an IOException if we wrote a code other than 'OK'.
         * Increments the appropriate statistics also.
         */
        @Override
        protected void processWrittenHeaders() throws IOException {
            if(outgoing) {
                switch(response.getStatusCode()) {
                case HandshakeResponse.OK:
                    break;
                case HandshakeResponse.SLOTS_FULL:
                    throw NoGnutellaOkException.CLIENT_REJECT;
                case HandshakeResponse.LOCALE_NO_MATCH:
                    //if responder's locale preferencing was set 
                    //and didn't match the locale this code is used.
                    //(currently in use by the dedicated connectionfetcher)
                    throw NoGnutellaOkException.CLIENT_REJECT_LOCALE;
                default:
                    throw NoGnutellaOkException.createClientUnknown(response.getStatusCode());
                }
            } else {
               switch(response.getStatusCode()) {
                   case HandshakeResponse.OK:
                   case HandshakeResponse.CRAWLER_CODE: // let the crawler IOX in ReadResponse
                        break;
                    case HandshakeResponse.SLOTS_FULL:
                        throw NoGnutellaOkException.CLIENT_REJECT;
                    default:
                        throw NoGnutellaOkException.createClientUnknown(response.getStatusCode());
                }
            }
        }
    }
    
    /** The first state in an outgoing handshake. */
    static class WriteRequestState extends WriteHandshakeState {
        private Properties request;
        
        /** Creates a new WriteRequestState using the given support & initial set of properties. */
        WriteRequestState(HandshakeSupport support, Properties request) {
            super(support);
            this.request = request;
        }

        /** Returns a ByteBuffer of the initial connect request & headers. */
        @Override
        protected ByteBuffer createOutgoingData() {
            StringBuilder sb = new StringBuilder();
            support.appendConnectLine(sb);
            support.appendHeaders(request, sb);
            return ByteBuffer.wrap(StringUtils.toAsciiBytes(sb.toString()));
        }
        
        /** Does nothing. */
        @Override
        protected void processWrittenHeaders() {}
    }
    
}
