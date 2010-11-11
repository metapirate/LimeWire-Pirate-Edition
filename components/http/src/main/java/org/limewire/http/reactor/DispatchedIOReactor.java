package org.limewire.http.reactor;

import java.net.Socket;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.IOReactor;
import org.apache.http.nio.reactor.ListeningIOReactor;

/**
 * An {@link IOReactor} that is analogous to {@link ListeningIOReactor}, except
 * it uses sockets that already a word (ie, 'GET', or 'HEAD') read off them,
 * and it does not listen directly on any ServerSockets.
 */
public interface DispatchedIOReactor extends IOReactor {
    
    /**
     * Notification that a socket is available, with the given word already
     * read off of it.  This returns the connection that is used to process
     * the given Socket.
     */
    NHttpConnection acceptConnection(String word, Socket socket);

}
