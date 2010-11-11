package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import org.limewire.core.settings.ApplicationSettings;

/**
 * A very simple responder to be used by leaf-nodes during the connection
 * handshake while accepting incoming connections.
 */
public final class LeafHandshakeResponder extends DefaultHandshakeResponder {

    private final HeadersFactory headersFactory;

    private final HandshakeServices handshakeServices;

    /**
     * Creates a new instance of LeafHandshakeResponder.
     * 
     * @param host the host with whom we are handshaking
     */
    LeafHandshakeResponder(String host, HeadersFactory headersFactory,
            HandshakeServices handshakeServices) {
        super(host);

        this.headersFactory = headersFactory;
        this.handshakeServices = handshakeServices;
    }

    /**
     * Responds to an outgoing connection handshake.
     * 
     * @return the <tt>HandshakeResponse</tt> with the handshake headers to send
     *         in response to the connection attempt
     */
    @Override
    protected HandshakeResponse respondToOutgoing(HandshakeResponse response) {

        // only connect to ultrapeers.
        if (!response.isUltrapeer()) {
            return HandshakeResponse
                    .createLeafRejectOutgoingResponse(HandshakeStatus.WE_ARE_LEAVES);
        }

        // check if this is a preferenced connection
        if (getLocalePreferencing()) {
            /*
             * TODO: ADD STAT
             * HandshakingStat.LEAF_OUTGOING_REJECT_LOCALE.incrementStat();
             */
            if (!ApplicationSettings.LANGUAGE.get().equals(response.getLocalePref())) {
                return HandshakeResponse.createLeafRejectLocaleOutgoingResponse();
            }
        }

        HandshakeStatus status = handshakeServices.getHandshakeStatusForResponse(response);
        if (!status.isAcceptable()) {
            return HandshakeResponse.createLeafRejectOutgoingResponse(status);
        }

        Properties ret = new Properties();

        // might as well save a little bandwidth.
        if (response.isDeflateAccepted()) {
            ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
        }

        return HandshakeResponse.createAcceptOutgoingResponse(ret);
    }

    /**
     * Responds to an incoming connection handshake.
     * 
     * @return the <tt>HandshakeResponse</tt> with the handshake headers to send
     *         in response to the connection attempt
     */
    @Override
    protected HandshakeResponse respondToIncoming(HandshakeResponse hr) {
        if (hr.isCrawler()) {
            return HandshakeResponse.createCrawlerResponse(handshakeServices);
        }

        // if not an ultrapeer, reject.
        if (!hr.isUltrapeer()) {
            return HandshakeResponse
                    .createLeafRejectOutgoingResponse(HandshakeStatus.WE_ARE_LEAVES);
        }

        Properties ret = headersFactory.createLeafHeaders(getRemoteIP());

        // If we already have enough ultrapeers, reject.
        HandshakeStatus status = handshakeServices.getHandshakeStatusForResponse(hr);
        if (!status.isAcceptable()) {
            return HandshakeResponse
                    .createLeafRejectIncomingResponse(hr, status, handshakeServices);
        }

        // deflate if we can ...
        if (hr.isDeflateAccepted()) {
            ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
        }

        // b) We're not a leaf yet, so accept the incoming connection
        return HandshakeResponse.createAcceptIncomingResponse(hr, ret, handshakeServices);
    }
}