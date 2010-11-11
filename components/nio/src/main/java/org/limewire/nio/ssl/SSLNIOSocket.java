package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;

/**
 * A <code>NIOSocket</code> that uses SSL for transfer encoding.
 */
public class SSLNIOSocket extends AbstractSSLSocket {

    public SSLNIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public SSLNIOSocket(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }

    public SSLNIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public SSLNIOSocket(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }

    public SSLNIOSocket() throws IOException {
        super();
    }
    
    SSLNIOSocket(Socket socket) {
        super(socket);
    }
    
    @Override
    protected SSLContext getSSLContext() {
        return SSLUtils.getSSLContext();
    }
    
    @Override
    protected String[] getCipherSuites() {
        return null; // SSLUtils.getSSLCipherSuites();
    }
}
