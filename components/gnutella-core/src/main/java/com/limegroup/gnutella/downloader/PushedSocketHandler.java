package com.limegroup.gnutella.downloader;

import java.net.Socket;

/** Handler for sockets that have been negotiated through a push. */
public interface PushedSocketHandler {

    /**
     * Accept the newly-connected push socket.
     * The given file, index & clientGUID were identified during the push.
     * 
     * @return true if the handler accepted the socket
     */
    public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket);

}