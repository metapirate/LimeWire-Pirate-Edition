package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.NIOSocket;
import org.limewire.nio.channel.ChannelReader;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.ThrottleReader;
import org.limewire.nio.observer.ConnectObserver;

/**
 * An {@link NIOSocket} that uses SSL/TLS for transfer encoding.
 * <p>
 * {@link AbstractSSLSocket} can be configured to support any cipher suite
 * and {@link SSLContext}.
 */
public abstract class AbstractSSLSocket extends NIOSocket {

    private final static Log LOG = LogFactory.getLog(TLSNIOSocket.class);
    
    private volatile SSLReadWriteChannel sslLayer;
    private volatile InterestReadableByteChannel baseReader;
    private volatile InterestWritableByteChannel baseWriter;

    public AbstractSSLSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public AbstractSSLSocket(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }

    public AbstractSSLSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    public AbstractSSLSocket(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }

    public AbstractSSLSocket() throws IOException {
        super();
    }
    
    AbstractSSLSocket(Socket socket) {
        super(socket);
    }
    
    /**
     * Returns the {@link SSLContext} that should be used to generate new
     * {@link SSLEngine SSLEngines}.
     */
    protected abstract SSLContext getSSLContext();
    
    /**
     * Returns the {@link Executor} that should be used to process long-lived tasks
     * generated from an {@link SSLEngine}.
     * 
     * @see {@link HandshakeStatus#NEED_TASK}
     */
    protected Executor getSSLExecutor() {
        return SSLUtils.getExecutor();
    }
    
    /**
     * Returns the {@link ByteBufferCache} that should be used to retrieve &
     * return {@link ByteBuffer ByteBuffers} for use reading & writing data.
     */
    protected ByteBufferCache getByteBufferCache() {
        return NIODispatcher.instance().getBufferCache();
    }

    /**
     * Returns the {@link Executor} that should be used to write & read data to
     * & from the network.
     */
    protected Executor getNetworkExecutor() {
        return NIODispatcher.instance().getScheduledExecutorService();
    }
    
    /**
     * Returns the cipher suites that are allowed to be used by the {@link SSLEngine}.
     * A return value of null means that the default cipher suites are enabled.
     * 
     * @see SSLEngine#getEnabledCipherSuites()
     * @see SSLEngine#getSupportedCipherSuites()
     */
    protected abstract String[] getCipherSuites();
    
    
    @Override
    public boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) {
        return super.connect(addr, timeout, new SSLConnectInitializer(addr, observer));
    }
    
    @Override
    protected InterestReadableByteChannel getBaseReadChannel() {
        if(baseReader == null) {
            sslLayer.setReadChannel(super.getBaseReadChannel());
            baseReader = sslLayer;
        }
        return baseReader;
    }

    @Override
    protected InterestWritableByteChannel getBaseWriteChannel() {
        if(baseWriter == null) {
            sslLayer.setWriteChannel(super.getBaseWriteChannel());
            baseWriter = sslLayer;
        }
        return baseWriter;
    }
    
    @Override
    protected void installThrottle(ThrottleReader throttle, ChannelReader reader) {
        // The goal is to insert the throttle such that
        // READER -> READER -> SSL LAYER -> THROTTLE -> SOCKET
        // so... do everything the same as the super, except when connecting
        // the throttle to the socket we don't connect it to the SSL layer,
        // instead we connect it to the real socket.
        
        ChannelReader lastChannel = reader;
        // go down the chain of ChannelReaders and find the last one to set our source
        while(lastChannel.getReadChannel() instanceof ChannelReader) {
            lastChannel = (ChannelReader)lastChannel.getReadChannel();
        }
        
        if(throttle != lastChannel) {
            lastChannel.setReadChannel(throttle);
            throttle.setReadChannel(super.getBaseReadChannel());
        }
    }
    
    @Override
    protected void initIncomingSocket() {
        super.initIncomingSocket();
        sslLayer = new SSLReadWriteChannel(getSSLContext(), getSSLExecutor(), getByteBufferCache(), getNetworkExecutor());
        sslLayer.initialize(getRemoteSocketAddress(), SSLUtils.getTLSCipherSuites(), false, false);
    }

    @Override
    protected void initOutgoingSocket() throws IOException {
        super.initOutgoingSocket();
        sslLayer = new SSLReadWriteChannel(getSSLContext(), getSSLExecutor(), getByteBufferCache(), getNetworkExecutor());
    }
    
    @Override
    protected void shutdownObservers() {
        if(sslLayer != null)
            sslLayer.shutdown();
        super.shutdownObservers();
    }
    
    /* package */ SSLReadWriteChannel getSSLChannel() {
        return sslLayer;
    }
    
    @Override
    /* Overridden to retrieve the soTimeout from the socket if we're still handshaking. */
    public long getReadTimeout() {
        if(sslLayer != null && sslLayer.isHandshaking()) {
            try {
                return getSoTimeout();
            } catch(SocketException se) {
                return 0;
            }
        } else {
            return super.getReadTimeout();
        }
    }
    
    /**
     * A delegating connector that forces the TLS Layer to be initialized
     * prior to informing the real <code>ConnectObserver</code> about the connection.
     */
    private class SSLConnectInitializer implements ConnectObserver {
        private final ConnectObserver delegate;
        private final SocketAddress addr;
        
        public SSLConnectInitializer(SocketAddress addr, ConnectObserver delegate) {
            this.delegate = delegate;
            this.addr = addr;
        }

        public void handleConnect(Socket socket) throws IOException {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Initializing SSL/TLS connection to " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + sslLayer.isOpen() +
                        ", handshaking " + sslLayer.isHandshaking());
            }
            sslLayer.initialize(addr, getCipherSuites(), true, false);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Initialized SSL/TLS connection to " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + sslLayer.isOpen() +
                        ", handshaking " + sslLayer.isHandshaking());
            }
            delegate.handleConnect(socket);
        }

        public void handleIOException(IOException iox) {
            if(LOG.isDebugEnabled()) {
                LOG.debug(iox + ", " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + sslLayer.isOpen() +
                        ", handshaking " + sslLayer.isHandshaking());
            }
            delegate.handleIOException(iox);
        }

        public void shutdown() {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Shutting down SSL/TLS connection to " +
                        getInetAddress().getHostAddress() + ":" + getPort() + 
                        ", open " + sslLayer.isOpen() +
                        ", handshaking " + sslLayer.isHandshaking());
            }
            delegate.shutdown();
        }        
    }
}
