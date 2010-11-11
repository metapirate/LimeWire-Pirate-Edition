package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.ProtocolBandwidthTracker;
import org.limewire.nio.channel.BufferReader;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.util.BufferUtils;

import com.google.inject.Provider;

/** Contains a collection of SSL-related utilities. */
public class SSLUtils {
    
    private SSLUtils() {}
        
    private static final Executor TLS_PROCESSOR = ExecutorsHelper.newProcessingQueue("TLSProcessor");
    private static final Provider<SSLContext> TLS_CONTEXT = new AbstractLazySingletonProvider<SSLContext>() {
        @Override
        protected SSLContext createObject() {
                try {
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, null, null);
                    // TODO: Set the SSLSessionContext cache size, or timeout?
                    return context;
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                } catch (KeyManagementException e) {
                    throw new IllegalStateException(e);
                }
        }        
    };
    private static final Provider<SSLContext> SSL_CONTEXT = new AbstractLazySingletonProvider<SSLContext>() {
        @Override
        protected SSLContext createObject() {
                try {
                    SSLContext context = SSLContext.getInstance("SSL");
                    context.init(null, null, null);
                    // TODO: Set the SSLSessionContext cache size, or timeout?
                    return context;
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                } catch (KeyManagementException e) {
                    throw new IllegalStateException(e);
                }
        }        
    };
    
    /** Returns the TLS cipher suites this generally supports. */
    public static String[] getTLSCipherSuites() {
        return new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" };
    }
    
    /** Returns the shared Executor for processing tasks from the SSLEngine. */
    public static Executor getExecutor() {
        return TLS_PROCESSOR;
    }
    
    /** Returns the shared SSL context. */
    public static SSLContext getSSLContext() {
        return SSL_CONTEXT.get();
    }
    
    /** Returns the shared TLS context. */
    public static SSLContext getTLSContext() {
        return TLS_CONTEXT.get();
    }
    
    /** Returns <code>true</code> is the given socket is already using TLS. */
    public static boolean isTLSEnabled(Socket socket) {
        return socket instanceof TLSNIOSocket;
    }
    
    /** Returns <code>true</code> if you are capable of performing a 
     * {@link #startTLS(Socket, ByteBuffer)} operation on this 
     * <code>socket</code>. */
    public static boolean isStartTLSCapable(Socket socket) {
        return socket instanceof AbstractNBSocket;
    }
    
    /**
     * Wraps an existing socket in a TLS-enabled socket.
     * Any data within 'data' will be pushed into the TLS layer.
     * <p>
     * This currently only works for creating server-side TLS sockets.
     * <p>
     * You must ensure that <code>isStartTLSCapable</code> returns true for the socket,
     * otherwise an <code>IllegalArgumentException</code> is thrown.
     */ 
    public static TLSNIOSocket startTLS(Socket socket, ByteBuffer data) throws IOException {
        if(socket instanceof AbstractNBSocket) {
            TLSNIOSocket tlsSocket = new TLSNIOSocket(socket);
            // Tell the channel to read in the buffered data.
            if(data.hasRemaining()) {
                SSLReadWriteChannel sslChannel = tlsSocket.getSSLChannel();
                InterestReadableByteChannel oldReader = sslChannel.getReadChannel();
                sslChannel.setReadChannel(new BufferReader(data));
                sslChannel.read(BufferUtils.getEmptyBuffer());
                if(data.hasRemaining())
                    throw new IllegalStateException("unable to read all prebuffered data in one pass!");
                sslChannel.setReadChannel(oldReader);
            }
            return tlsSocket;
        } else {
            throw new IllegalArgumentException("cannot wrap non AbstractNBSocket");
        }
    }
    
    /**
     * Returns a tracker for the given socket.
     * If no SSL exchanges are performed on the socket, the returned
     * tracker will always report 0 bytes produced and consumed.
     */
    public static ProtocolBandwidthTracker getSSLBandwidthTracker(Socket socket) {
        if(socket instanceof TLSNIOSocket) {
           return new SSLChannelTracker(((TLSNIOSocket)socket).getSSLChannel());
        } else {
            return EmptyTracker.instance();
        }
    }
    /**
     * Provides an implementation for the {@link ProtocolBandwidthTracker}
     * interface where each "get" method returns 
     * zero. Using an empty tracker as the default value of a 
     * <code>SSLBandwidthTracker</code> avoids 
     * <code>NullPointerExceptions</code>.
     */
    public static class EmptyTracker implements ProtocolBandwidthTracker {
        private static final EmptyTracker instance = new EmptyTracker();
        public static final EmptyTracker instance() { return instance; }
        private EmptyTracker() {}
        public long getReadBytesConsumed() { return 0; }
        public long getReadBytesProduced() { return 0; }
        public long getWrittenBytesConsumed() { return 0; }
        public long getWrittenBytesProduced() { return 0; }
    }
    
    private static class SSLChannelTracker implements ProtocolBandwidthTracker {
        private final SSLReadWriteChannel channel;
        
        SSLChannelTracker(SSLReadWriteChannel channel) {
            this.channel = channel;
        }

        public long getReadBytesConsumed() {
            return channel.getReadBytesConsumed();
        }

        public long getReadBytesProduced() {
            return channel.getReadBytesProduced();
        }

        public long getWrittenBytesConsumed() {
            return channel.getWrittenBytesConsumed();
        }

        public long getWrittenBytesProduced() {
            return channel.getWrittenBytesProduced();
        }
    }
}
