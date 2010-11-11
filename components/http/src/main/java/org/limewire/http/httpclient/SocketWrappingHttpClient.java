package org.limewire.http.httpclient;

import java.net.Socket;


/**
 * An <code>HttpClient</code> that uses a <code>Socket</code> specified by the caller.
 * This is useful for http that is initiated via the "server side", such as a 
 * GIV
 */
public interface SocketWrappingHttpClient extends LimeHttpClient {

    /**
     * Sets the socket to use
     * @param socket the socket to use
     */
    public void setSocket(Socket socket);
}
