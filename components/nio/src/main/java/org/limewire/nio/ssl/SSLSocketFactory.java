package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.nio.NBSocketFactory;

/** 
 * An <code>SSLSocketFactory</code> that returns 
 * <a href="http://en.wikipedia.org/wiki/Secure_Sockets_Layer">SSL</a> sockets. 
 */
public class SSLSocketFactory extends NBSocketFactory {

    @Override
    public SSLNIOSocket createSocket() throws IOException {
        return new SSLNIOSocket();
    }

    @Override
    public SSLNIOSocket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        return new SSLNIOSocket(host, port);
    }

    @Override
    public SSLNIOSocket createSocket(InetAddress host, int port) throws IOException {
        return new SSLNIOSocket(host, port);
    }

    @Override
    public SSLNIOSocket createSocket(String host, int port, InetAddress localHost,
            int localPort) throws IOException, UnknownHostException {
        return new SSLNIOSocket(host, port, localHost, localPort);
    }

    @Override
    public SSLNIOSocket createSocket(InetAddress address, int port,
            InetAddress localAddress, int localPort) throws IOException {
        return new SSLNIOSocket(address, port, localAddress, localPort);
    }

}
