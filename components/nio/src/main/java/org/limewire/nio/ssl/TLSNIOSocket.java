package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;

/**
 * A <code>NIOSocket</code> that uses TLS for transfer encoding.
 * <p>
 * <code>TLSNIOSocket</code> is currently hardcoded to only support the cipher suite
 * <code>TLS_DH_anon_WITH_AES_128_CBC_SHA</code>.
 */
public class TLSNIOSocket extends AbstractSSLSocket {

    public TLSNIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public TLSNIOSocket(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }

    public TLSNIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public TLSNIOSocket(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }

    public TLSNIOSocket() throws IOException {
        super();
    }
    
    TLSNIOSocket(Socket socket) {
        super(socket);
    }
    
    @Override
    protected SSLContext getSSLContext() {
        return SSLUtils.getTLSContext();
    }
    
    @Override
    protected String[] getCipherSuites() {
        return SSLUtils.getTLSCipherSuites();
    }
}
