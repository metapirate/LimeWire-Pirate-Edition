package com.limegroup.gnutella.handshaking;

import java.io.IOException;

/**
 * Exception thrown when someone understands responds with a handshaking
 * code other than 200 or 401.
 */
public final class NoGnutellaOkException extends IOException {

    /**
     * Constant for whether or not the <tt>NoGnutellaOkException</tt>
     * came from us.
     */
    private final boolean wasMe;

    /**
     * Constant for the status code of the handshake header that 
     * caused the exception.
     */
    private final int code;

    /**
     * Constant for the default message for exceptions due to fatal
     * server responses.
     */
    private static final String FATAL_SERVER_MSG =
        "Server sent fatal response: ";        

    /**
     * Constant for the default message for exceptions due to fatal
     * responses from us when we reject connections.
     */
    private static final String FATAL_CLIENT_MSG =
        "We sent fatal response: ";        

    /**
     * Cached <tt>NoGnutellaOkException</tt> for the case where
     * the server rejected the connection with a 503.
     */
    public static final NoGnutellaOkException SERVER_REJECT =
        new NoGnutellaOkException(false, HandshakeResponse.SLOTS_FULL, 
                                  FATAL_SERVER_MSG+
                                  HandshakeResponse.SLOTS_FULL);

    /**
     * Cached <tt>NoGnutellaOkException</tt> for the case where
     * we as the client are rejecting the connection with a 503.
     */
    public static final NoGnutellaOkException CLIENT_REJECT =
        new NoGnutellaOkException(false, HandshakeResponse.SLOTS_FULL, 
                                  FATAL_CLIENT_MSG+
                                  HandshakeResponse.SLOTS_FULL);

    /**
     * Reject exception for the case when a connection is rejected 
     * due to unmatching locales.
     */
    public static final NoGnutellaOkException CLIENT_REJECT_LOCALE =
        new NoGnutellaOkException(false,
                                  HandshakeResponse.LOCALE_NO_MATCH,
                                  FATAL_CLIENT_MSG + 
                                  HandshakeResponse.LOCALE_NO_MATCH);
    /**
     * Cached <tt>NoGnutellaOkException</tt> for the case where
     * the handshake never resolved successfully on the cleint
     * side.
     */
    public static final NoGnutellaOkException UNRESOLVED_CLIENT =
        new NoGnutellaOkException(true,
                                  "Too much handshaking, no conclusion");

    /**
     * Cached <tt>NoGnutellaOkException</tt> for the case where
     * the handshake never resolved successfully on the server
     * side.
     */
    public static final NoGnutellaOkException UNRESOLVED_SERVER =
        new NoGnutellaOkException(false,
                                  "Too much handshaking, no conclusion");


    /**
     * Creates a new <tt>NoGnutellaOkException</tt> from an unknown
     * client response.
     *
     * @param code the response code from the server
     */
    public static NoGnutellaOkException createClientUnknown(int code) {
        return new NoGnutellaOkException(true, code,
                                         FATAL_SERVER_MSG+code);
    }

    /**
     * Creates a new <tt>NoGnutellaOkException</tt> from an unknown
     * server response.
     *
     * @param code the response code from the server
     */
    public static NoGnutellaOkException createServerUnknown(int code) {
        return new NoGnutellaOkException(false, code,
                                         FATAL_SERVER_MSG+code);
    }

    /**
     * @param wasMe true if I returned the non-standard code.
     *  False if the remote host did.
     * @param code non-standard code
     * @param message a human-readable message for debugging purposes
     *  NOT necessarily the message given during the interaction.
     */
    private NoGnutellaOkException(boolean wasMe, 
                                  int code,
                                  String message) {
        super(message);
        this.wasMe=wasMe;
        this.code=code;
    }

    /**
     * Constructor for codeless exception.
     */
    private NoGnutellaOkException(boolean wasMe, String message) {
        this(wasMe, -1, message);
    }
    
    /** 
     * Returns true if the exception was caused by something this host
     * wrote. 
     */
    public boolean wasMe() {
        return wasMe;
    }

    /**
     * The offending status code.
     */
    public int getCode() {
        return code;
    }

}

