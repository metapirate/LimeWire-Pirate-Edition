package org.limewire.http.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.SessionRequestImpl;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.NBSocket;
import org.limewire.nio.observer.ConnectObserver;

public class LimeConnectingIOReactor implements ConnectingIOReactor {

    public static final String IO_SESSION_KEY = "org.limewire.iosession";

    //private static final Log LOG = LogFactory.getLog(LimeConnectingIOReactor.class);
    
    private final HttpParams params;
    
    protected volatile IOEventDispatch eventDispatch = null;
    
    private final Executor ioExecutor;
    private final SocketsManager socketsManager;
    
    private final HttpBandwidthTracker up, down;
    
    public LimeConnectingIOReactor(final HttpParams params, final Executor ioExecutor,
            SocketsManager socketsManager, HttpBandwidthTracker up, HttpBandwidthTracker down) {
        if (params == null) {
            throw new IllegalArgumentException();
        }
        
        this.params = params;
        this.ioExecutor = ioExecutor;
        this.socketsManager = socketsManager;
        this.up = up;
        this.down = down;
    }
    
    public LimeConnectingIOReactor(final HttpParams params, final Executor ioExecutor,
            SocketsManager socketsManager) {
        this(params, ioExecutor,socketsManager, new HttpBandwidthTracker(), new HttpBandwidthTracker());
    }
    
    public void execute(IOEventDispatch eventDispatch) throws IOException {
        if (!(eventDispatch instanceof DefaultClientIOEventDispatch)) {
            throw new IllegalArgumentException("Event dispatch must be of type DefaultClientIOEventDispatch");
        }
        this.eventDispatch = eventDispatch;
    }
    
    public float getMeasuredBandwidth(boolean downstream) {
            if (downstream) {
                down.measureBandwidth();
                return down.getMeasuredBandwidth();
            } else {
                up.measureBandwidth();
                return up.getMeasuredBandwidth();
            }
    }
    
    public SessionRequest connect(SocketAddress remoteAddress, 
            SocketAddress localAddress, 
            final Object attachment,
            SessionRequestCallback callback) {
        
        // TODO: use custom impl that implements cancel & fails on setConnectionTimeout?
        final SessionRequestImpl sessionRequest = new SessionRequestImpl(
                remoteAddress, localAddress, attachment, callback);
        
        sessionRequest.setConnectTimeout(HttpConnectionParams.getConnectionTimeout(this.params));
        
        NBSocket socket;
        try {
            socket = (NBSocket) socketsManager.create(ConnectType.PLAIN);
            socketsManager.connect(socket, (InetSocketAddress) localAddress,
                    (InetSocketAddress) remoteAddress, sessionRequest.getConnectTimeout(),
                    new ConnectObserver() {
                        public void handleConnect(Socket socket) throws IOException {
                            prepareSocket((AbstractNBSocket)socket, attachment, sessionRequest);
                        }

                        public void handleIOException(IOException iox) {
                            sessionRequest.failed(iox);
                        }

                        public void shutdown() {
                            sessionRequest.failed(new IOException("couldn't connect"));
                        }
                    }, ConnectType.PLAIN);
        } catch (IOException iox) {
            sessionRequest.failed(iox);
        }
        
        return sessionRequest;
    }
    

    /**
     * Sets parameters of <code>socket</code> based on default {@link HttpParams},
     * and attachs to the NIO layer. 
     */
    protected void prepareSocket(AbstractNBSocket socket, Object attachment, SessionRequestImpl sessionRequest) throws IOException {
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(this.params));
        int linger = HttpConnectionParams.getLinger(this.params);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
        
        final HttpIOSession session = new HttpIOSession(socket, ioExecutor); 
        
        session.setAttribute(IOSession.ATTACHMENT_KEY, attachment);
        session.setSocketTimeout(HttpConnectionParams.getSoTimeout(this.params));
        
        HttpChannel channel = new HttpChannel(session, eventDispatch, null, up, down);
        session.setHttpChannel(channel);
        
        eventDispatch.connected(session);
        sessionRequest.completed(session);
        
        // need to enable access to the channel for throttling support
        // TODO: necessary?
        NHttpConnection conn = (NHttpConnection) session.getAttribute(ExecutionContext.HTTP_CONNECTION);
        assert conn != null;
        conn.getContext().setAttribute(IO_SESSION_KEY, session);
        
        // TODO: do something about ThrottleReader
        socket.setReadObserver(channel);
        socket.setWriteObserver(channel);
    }


    /** 
     * Throws {@link UnsupportedOperationException}.
     */
    public IOReactorStatus getStatus() {
        throw new UnsupportedOperationException();
    }

    /** 
     * 
     */
    public void shutdown() throws IOException {
        
    }

    /** 
     * Throws {@link UnsupportedOperationException}.
     */
    public void shutdown(long gracePeriod) throws IOException {
        throw new UnsupportedOperationException();
    }

}
