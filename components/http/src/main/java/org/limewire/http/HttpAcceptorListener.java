package org.limewire.http;

import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;

/**
 * Defines the requirements for classes that listen to events sent by
 * {@link BasicHttpAcceptor}.
 */
public interface HttpAcceptorListener {

    /**
     * Invoked when a new HTTP connection has been established before the first
     * request is received.
     */
    void connectionClosed(NHttpConnection conn);

    /**
     * Invoked when a HTTP connection has been closed.
     */
    void connectionOpen(NHttpConnection conn);

    /**
     * Invoked when a response has been sent.
     */
    void responseSent(NHttpConnection conn, HttpResponse response);
    
}
