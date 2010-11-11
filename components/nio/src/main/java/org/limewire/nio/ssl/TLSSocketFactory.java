package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.nio.NBSocketFactory;

/** 
 * An <code>NBSocketFactory</code> that returns 
 * <a href="http://en.wikipedia.org/wiki/Secure_Sockets_Layer">TLS</a> sockets. 
 */
public class TLSSocketFactory extends NBSocketFactory {

    @Override
    public TLSNIOSocket createSocket() throws IOException {
        return new TLSNIOSocket();
    }

    @Override
    public TLSNIOSocket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        return new TLSNIOSocket(host, port);
    }

    @Override
    public TLSNIOSocket createSocket(InetAddress host, int port) throws IOException {
        return new TLSNIOSocket(host, port);
    }

    @Override
    public TLSNIOSocket createSocket(String host, int port, InetAddress localHost,
            int localPort) throws IOException, UnknownHostException {
        return new TLSNIOSocket(host, port, localHost, localPort);
    }

    @Override
    public TLSNIOSocket createSocket(InetAddress address, int port,
            InetAddress localAddress, int localPort) throws IOException {
        return new TLSNIOSocket(address, port, localAddress, localPort);
    }

}
