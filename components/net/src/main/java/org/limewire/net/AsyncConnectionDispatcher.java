package org.limewire.net;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.AbstractChannelInterestReader;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.util.BufferUtils;
import org.limewire.util.StringUtils;

/**
 * A ConnectionDispatcher that reads asynchronously from the socket.
 */
public class AsyncConnectionDispatcher extends AbstractChannelInterestReader {
    
    private static final Log LOG = LogFactory.getLog(AsyncConnectionDispatcher.class);

    private final ConnectionDispatcher dispatcher;
    private final Socket socket;
    private final String allowedWord;
    private final boolean allowTLS;
    private boolean finished = false;
     
    public AsyncConnectionDispatcher(ConnectionDispatcher dispatcher, Socket socket, String allowedWord, boolean allowTLS) {
        // + 1 for whitespace
        super(dispatcher.getMaximumWordSize() + 1);
 
        if (socket == null) {
            throw new IllegalArgumentException();
        }
        
        this.dispatcher = dispatcher;
        this.socket = socket;
        this.allowedWord = allowedWord;
        this.allowTLS = allowTLS;
    }
    
    public void handleRead() throws IOException {
        // If we already finished our reading, turn read interest off
        // and exit early.
        if(finished) {
            source.interestRead(false);
            return;
        }
        
        // Fill up our buffer as much we can.
        int read = 0;
        while(buffer.hasRemaining() && (read = source.read(buffer)) > 0);
        
        // See if we have a full word.
        for(int i = 0; i < buffer.position(); i++) {
            if(buffer.get(i) == ' ') {                
                String word = StringUtils.getASCIIString(buffer.array(), 0, i);
                if(dispatcher.isValidProtocolWord(word)) {
                    if(allowedWord != null && !allowedWord.equals(word)) {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Legal but wrong word: " + word);
                        throw new IOException("wrong word!");
                    }

                    if(LOG.isDebugEnabled())
                        LOG.debug("Dispatching word: " + word);
                    buffer.limit(buffer.position()).position(i+1);
                    source.interestRead(false);
                    dispatcher.dispatch(word, socket, true);
                } else {
                    startTLS();
                }
                finished = true;
                return;
            }
        }
        
        // If there's no room to read more or there's nothing left to read,
        // we aren't going to read our word.  Attempt to switch to TLS, or
        // close if we EOF'd early.
        if(!buffer.hasRemaining()) {
            startTLS();
            finished = true;
            return;
        } else if(read == -1) {
            close();
            return;
        }
    }
    
    /**
     * Attempts to start TLS encoding on the socket.
     * If any data was buffered but not used, the data will be read as part
     * of the TLS handshake.  If the socket is not capable of switching to TLS,
     * the socket is closed.
     * 
     * @throws IOException if there was an error starting TLS
     */
    private void startTLS() throws IOException {
        if (allowTLS && !SSLUtils.isTLSEnabled(socket) && SSLUtils.isStartTLSCapable(socket)) {
            LOG.debug("Attempting to start TLS");
            buffer.flip();
            AbstractNBSocket tlsSocket = SSLUtils.startTLS(socket, buffer);
            tlsSocket.setReadObserver(new AsyncConnectionDispatcher(dispatcher, tlsSocket, allowedWord, allowTLS));
        } else {
            close();
        }
    }
    
    @Override
    public int read(ByteBuffer dst) {
        return BufferUtils.transfer(buffer, dst, false);
    }

    @Override
    public long read(ByteBuffer [] dst) {
        return BufferUtils.transfer(buffer, dst, 0, dst.length, false);
    }
    
    @Override
    public long read(ByteBuffer [] dst, int offset, int length) {
        return BufferUtils.transfer(buffer, dst, offset, length, false);
    }
}
