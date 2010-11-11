package org.limewire.http.reactor;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.limewire.io.IOUtils;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.ThrottleWriter;

/**
 * An implementation of {@link DispatchedIOReactor} interface that
 * establishes connections through LimeWire's NIO layer.
 * <p>
 * This is analogous to {@link ListeningIOReactor}.
 */
public class DefaultDispatchedIOReactor implements DispatchedIOReactor {

    public static final String IO_SESSION_KEY = "org.limewire.iosession";

    private static final Log LOG = LogFactory.getLog(DefaultDispatchedIOReactor.class);
    
    private final HttpParams params;
    
    protected volatile IOEventDispatch eventDispatch = null;
    
    private final Executor ioExecutor;
    
    public DefaultDispatchedIOReactor(final HttpParams params, final Executor ioExecutor) {
        if (params == null) {
            throw new IllegalArgumentException();
        }
        
        this.params = params;
        this.ioExecutor = ioExecutor;
    }
    
    public void execute(IOEventDispatch eventDispatch) throws IOException {
        if (!(eventDispatch instanceof DefaultServerIOEventDispatch)) {
            throw new IllegalArgumentException("Event dispatch must be of type DefaultServerIOEventDispatch");
        }
        this.eventDispatch = eventDispatch;
    }

    /**
     * Sets parameters of <code>socket</code> based on default {@link HttpParams}. 
     */
    protected void prepareSocket(final Socket socket) throws IOException {
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(this.params));
        int linger = HttpConnectionParams.getLinger(this.params);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
    }
    
    /**
     * Connects <code>socket</code> to LimeWire's NIO layer. 
     */
    protected NHttpConnection connectSocket(AbstractNBSocket socket, Object attachment, String word) {
        final HttpIOSession session = new HttpIOSession(socket, ioExecutor);        
        
        session.setAttribute(IOSession.ATTACHMENT_KEY, attachment);
        session.setSocketTimeout(HttpConnectionParams.getSoTimeout(this.params));
        
        HttpChannel channel = new HttpChannel(session, eventDispatch, word);
        session.setHttpChannel(channel);

        ThrottleWriter throttleWriter = new ThrottleWriter(null);
        session.setThrottleChannel(throttleWriter);
        channel.setWriteChannel(throttleWriter);
        
        this.eventDispatch.connected(session);
        
        // need to enable access to the channel for throttling support
        NHttpConnection conn = (NHttpConnection) session.getAttribute(ExecutionContext.HTTP_CONNECTION);
        assert conn != null;
        conn.getContext().setAttribute(IO_SESSION_KEY, session);
        
        socket.setReadObserver(channel);
        socket.setWriteObserver(channel);
        
        return conn;
    }

    /**
     * Processes an established connection.
     * 
     * @param word the text that was send on connect, this is injected back when
     *        the socket's channel is read
     * @param socket the socket
     * @return the HttpCore connection object
     */
    public NHttpConnection acceptConnection(String word, Socket socket) {
        try {
            prepareSocket(socket);
            return connectSocket((AbstractNBSocket) socket, null, word);
        } catch (IOException e) {
            LOG.info("Closing socket due to unexpected exception", e);
            IOUtils.close(socket);
            return null;
        }
    }

    /** 
     * Throws {@link UnsupportedOperationException}.
     */
    public IOReactorStatus getStatus() {
        throw new UnsupportedOperationException();
    }

    /** 
     * Throws {@link UnsupportedOperationException}.
     */
    public void shutdown() throws IOException {
        throw new UnsupportedOperationException();
    }

    /** 
     * Throws {@link UnsupportedOperationException}.
     */
    public void shutdown(long gracePeriod) throws IOException {
        throw new UnsupportedOperationException();
    }

}
