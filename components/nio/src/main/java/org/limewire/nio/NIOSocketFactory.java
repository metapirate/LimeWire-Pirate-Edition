package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** An <code>NBSocketFactory</code> that returns <code>NIOSockets</code>. */
public class NIOSocketFactory extends NBSocketFactory {

    @Override
    public NIOSocket createSocket(String host, int port) throws IOException, UnknownHostException {
        return new NIOSocket(host, port);
    }

    @Override
    public NIOSocket createSocket(InetAddress host, int port) throws IOException {
        return new NIOSocket(host, port);
    }

    @Override
    public NIOSocket createSocket(String host, int port, InetAddress localHost,
            int localPort) throws IOException, UnknownHostException {
        return new NIOSocket(host, port, localHost, localPort);
    }

    @Override
    public NIOSocket createSocket(InetAddress address, int port,
            InetAddress localAddress, int localPort) throws IOException {
        return new NIOSocket(address, port, localAddress, localPort);
    }

    @Override
    public NIOSocket createSocket() throws IOException {
        return new NIOSocket();
    }

}
