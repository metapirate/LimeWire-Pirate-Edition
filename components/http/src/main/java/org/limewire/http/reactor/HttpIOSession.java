package org.limewire.http.reactor;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionBufferStatus;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.Throttle;
import org.limewire.nio.channel.ThrottleWriter;

/**
 * Provides an implementation of the <code>IOSession</code> interface that
 * connects to LimeWire's NIO layer and has support for throttling.
 */
public class HttpIOSession implements IOSession {

    private static final Log LOG = LogFactory.getLog(HttpIOSession.class);
    
    private final Map<String, Object> attributes;

    private SessionBufferStatus bufferStatus;

    private int socketTimeout;

    private AbstractNBSocket socket;

    private HttpChannel channel;

    private int eventMask;

    private ThrottleWriter throttleWriter;

    private AtomicBoolean closed = new AtomicBoolean(false);
    
    private final Executor ioExecutor;

    public HttpIOSession(AbstractNBSocket socket, Executor ioExecutor) {
        if (socket == null) {
            throw new IllegalArgumentException();
        }

        this.attributes = Collections
                .synchronizedMap(new HashMap<String, Object>());
        this.socketTimeout = 0;
        this.socket = socket;
        this.ioExecutor = ioExecutor;
    }

    public void setHttpChannel(HttpChannel channel) {
        this.channel = channel;
    }

    public ByteChannel channel() {
        return channel;
    }

    public void close() {
        if (this.closed.getAndSet(true)) {
            return;
        }
        channel.closeWhenBufferedOutputHasBeenFlushed();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public SessionBufferStatus getBufferStatus() {
        return bufferStatus;
    }

    public SocketAddress getLocalAddress() {
        return socket.getLocalSocketAddress();
    }

    public SocketAddress getRemoteAddress() {
        return socket.getRemoteSocketAddress();
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

    public void setAttribute(String name, Object obj) {
        attributes.put(name, obj);
    }

    public void setBufferStatus(SessionBufferStatus status) {
        this.bufferStatus = status;
    }

    public synchronized int getEventMask() {
        return eventMask;
    }

    public synchronized void setEventMask(int ops) {
        if (isClosed()) {
            if (LOG.isErrorEnabled())
                LOG.error("Attempted to set event mask to " + ops + " on closed session: " + this);
            return;
        }
        
        this.eventMask = ops;
        channel.requestRead((ops & EventMask.READ) != 0);
        channel.requestWrite((ops & EventMask.WRITE) != 0);

//        if ((ops & EventMask.READ) != 0) {
//            System.err.println("read on");
//        } else {
//            System.err.println("read off");            
//        }
//        if ((ops & EventMask.WRITE) != 0) {
//            System.err.println("write on");
//        } else {
//            System.err.println("write off");            
//        }
    }

    public synchronized void setEvent(int op) {
        if (isClosed()) {
            if (LOG.isErrorEnabled())
                LOG.error("Attempted to set event mask to " + op + " on closed session: " + this);
            return;
        }

        this.eventMask |= op;
        if ((op & EventMask.READ) != 0) {
            channel.requestRead(true);
        }
        if ((op & EventMask.WRITE) != 0) {
            channel.requestWrite(true);
        }        
        
//        if ((op & EventMask.READ) != 0) {
//            System.err.println("read on");
//        }
//        if ((op & EventMask.WRITE) != 0) {
//            System.err.println("write on");
//        }
    }

    public synchronized void clearEvent(int op) {
        if (isClosed()) {
            if (LOG.isErrorEnabled())
                LOG.error("Attempted to set event mask to " + op + " on closed session: " + this);
            return;
        }

        this.eventMask &= ~op;
        if ((op & EventMask.READ) != 0) {
            channel.requestRead(false);
        }
        if ((op & EventMask.WRITE) != 0) {
            channel.requestWrite(false);
        }

//        if ((op & EventMask.READ) != 0) {
//            System.err.println("read off");
//        }
//        if ((op & EventMask.WRITE) != 0) {
//            System.err.println("write off");
//        }
    }

    public void setSocketTimeout(int timeout) {
        this.socketTimeout = timeout;
        try {
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            LOG.warn("Could not set socket timeout", e);
        }
    }

    public boolean hasBufferedInput() {
        return this.bufferStatus != null
                && this.bufferStatus.hasBufferedInput();
    }

    public boolean hasBufferedOutput() {
        return this.bufferStatus != null
                && this.bufferStatus.hasBufferedOutput();
    }

    /**
     * Throttles the underlying connection using <code>throttle</code>. If
     * <code>throttle</code> is null, throttling is disabled.
     */
    public void setThrottle(final Throttle throttle) {
        assert NIODispatcher.instance().isDispatchThread() :
            "wrong thread: "+Thread.currentThread().getName();
    
        this.throttleWriter.setThrottle(throttle);
    }

    public Socket getSocket() {
        return socket;
    }

    public void shutdown() {
        closed.set(true);
        socket.close();
    }

    public void setThrottleChannel(final ThrottleWriter throttleWriter) {
        this.throttleWriter = throttleWriter;
    }

    public int getStatus() {
        throw new UnsupportedOperationException();
    }
    
    public Executor getIoExecutor() {
        return ioExecutor;
    }
    
}