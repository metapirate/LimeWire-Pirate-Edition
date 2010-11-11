package com.limegroup.gnutella.handshaking;

import java.io.IOException;


/**
 * Allows a handshaker to exist.
 */
public interface Handshaker {
    public void shake() throws IOException, BadHandshakeException, NoGnutellaOkException;
    public HandshakeResponse getWrittenHeaders();
    public HandshakeResponse getReadHeaders();
}
