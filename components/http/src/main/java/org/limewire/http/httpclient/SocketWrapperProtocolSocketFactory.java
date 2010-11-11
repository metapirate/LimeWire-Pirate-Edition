package org.limewire.http.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpParams;

import com.google.inject.Inject;

/**
 * A <code>SocketFactory</code> that can be given 
 * a <code>Socket</code> to use.
 */
public class SocketWrapperProtocolSocketFactory implements SocketFactory {

    private Socket socket;

    @Inject
    public SocketWrapperProtocolSocketFactory() {
    }

    public SocketWrapperProtocolSocketFactory(Socket socket) {
        this.socket = socket;
    }
    
    void setSocket(Socket s) {
        this.socket = s;
    }

    public Socket createSocket() throws IOException {
        return socket;
    }

    public Socket connectSocket(Socket socket, String s, int i, InetAddress inetAddress, int i1, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
        return socket; // TODO validate parameters actually match those of the socket
    }

    public boolean isSecure(Socket socket) throws IllegalArgumentException {
        return false; // TODO
    }
}
