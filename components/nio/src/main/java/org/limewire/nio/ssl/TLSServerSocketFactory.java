package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;

import org.limewire.nio.NBServerSocketFactory;
import org.limewire.nio.observer.AcceptObserver;

/** 
 * An <code>NBServerSocketFactory</code> that returns 
 * {@link TLSNIOServerSocket TLSNIOServerSockets}. 
 */
public class TLSServerSocketFactory extends NBServerSocketFactory {

    @Override
    public TLSNIOServerSocket createServerSocket() throws IOException {
        return new TLSNIOServerSocket();
    }

    @Override
    public TLSNIOServerSocket createServerSocket(AcceptObserver observer)
            throws IOException {
        return new TLSNIOServerSocket(observer);
    }

    @Override
    public TLSNIOServerSocket createServerSocket(int port, AcceptObserver observer)
            throws IOException {
        return new TLSNIOServerSocket(port, observer);
    }

    @Override
    public TLSNIOServerSocket createServerSocket(int port, int backlog,
            AcceptObserver observer) throws IOException {
        return new TLSNIOServerSocket(port, backlog, observer);
    }

    @Override
    public TLSNIOServerSocket createServerSocket(int port, int backlog,
            InetAddress bindAddr, AcceptObserver observer) throws IOException {
        return new TLSNIOServerSocket(port, backlog, bindAddr, observer);
    }

    @Override
    public TLSNIOServerSocket createServerSocket(int port) throws IOException {
        return new TLSNIOServerSocket(port);
    }

    @Override
    public TLSNIOServerSocket createServerSocket(int port, int backlog)
            throws IOException {
        return new TLSNIOServerSocket(port, backlog);
    }

    @Override
    public TLSNIOServerSocket createServerSocket(int port, int backlog,
            InetAddress ifAddress) throws IOException {
        return new TLSNIOServerSocket(port, backlog, ifAddress);
    }

}
