package org.limewire.nio;
import java.io.IOException;
import java.net.Socket;

import javax.net.SocketFactory;

/** A <code>SocketFactory</code> that also allows you to create unconnected 
 * sockets. 
 */
public abstract class ExtendedSocketFactory extends SocketFactory {
    
    /** Returns a new unconnected socket. */
    @Override
    public abstract Socket createSocket() throws IOException;
    
}
