package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;

import org.limewire.nio.observer.AcceptObserver;

/** An <code>NBServerSocketFactory</code> that returns <code>NIOServerSockets</code>. */
public class NIOServerSocketFactory extends NBServerSocketFactory {

    @Override
    public NIOServerSocket createServerSocket(AcceptObserver observer)
            throws IOException {
        return new NIOServerSocket(observer);
    }

    @Override
    public NIOServerSocket createServerSocket(int port, AcceptObserver observer)
            throws IOException {
        return new NIOServerSocket(port, observer);
    }

    @Override
    public NIOServerSocket createServerSocket(int port, int backlog,
            AcceptObserver observer) throws IOException {
        return new NIOServerSocket(port, backlog, observer);
    }

    @Override
    public NIOServerSocket createServerSocket(int port, int backlog,
            InetAddress bindAddr, AcceptObserver observer) throws IOException {
        return new NIOServerSocket(port, backlog, bindAddr, observer);
    }

    @Override
    public NIOServerSocket createServerSocket(int port) throws IOException {
        return new NIOServerSocket(port);
    }

    @Override
    public NIOServerSocket createServerSocket(int port, int backlog)
            throws IOException {
        return new NIOServerSocket(port, backlog);
    }

    @Override
    public NIOServerSocket createServerSocket(int port, int backlog,
            InetAddress ifAddress) throws IOException {
        return new NIOServerSocket(port, backlog, ifAddress);
    }

}
