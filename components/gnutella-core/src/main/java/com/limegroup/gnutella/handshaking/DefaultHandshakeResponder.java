package com.limegroup.gnutella.handshaking;

/**
 * This is an abstract class that provides a default implementation of
 * HandshakeResponder.
 */
public abstract class DefaultHandshakeResponder implements HandshakeResponder {

    /**
     * The host to which are opening connection
     */
    private final String _host;

    /**
     * Whether the handshake responder should do locale preferencing.
     */
    private boolean _pref = false;

    /**
     * Creates a new instance.
     * 
     * @param manager instance of connection manager, managing this connection
     * @param host the host with whom we are handshaking
     */
    public DefaultHandshakeResponder(String host) {
        this._host = host;
    }

    /**
     * Calls respondToOutgoing or respondToIncoming based on the value of
     * outgoing.
     */
    public HandshakeResponse respond(HandshakeResponse response, boolean outgoing) {
        if (outgoing)
            return respondToOutgoing(response);
        return respondToIncoming(response);
    }

    /**
     * Returns the Remote IP.
     */
    protected String getRemoteIP() {
        return _host;
    }

    /**
     * Responds to the given outgoing HandshakeResponse.
     */
    protected abstract HandshakeResponse respondToOutgoing(HandshakeResponse response);

    /**
     * Responds to the given incoming HandshakeResponse.
     */
    protected abstract HandshakeResponse respondToIncoming(HandshakeResponse response);

    /**
     * Set locale preferencing.
     */
    public void setLocalePreferencing(boolean b) {
        _pref = b;
    }

    /**
     * Get locale preferencing.
     */
    public boolean getLocalePreferencing() {
        return _pref;
    }
}