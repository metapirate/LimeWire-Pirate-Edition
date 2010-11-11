package org.limewire.nio.ssl;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.ByteBufferCache;
import org.limewire.util.BufferUtils;

/** A simple test that connects two SSLEngines and ensures SSL works. */
public class SSLEngineTest {
    
    private static final Log LOG = LogFactory.getLog(SSLEngineTest.class);
    
    private final SSLContext context;
    private final String[] cipherSuites;
    private final ByteBufferCache cache;
    
    private Throwable lastFailureCause;
    
    public SSLEngineTest(SSLContext context, String[] cipherSuites, ByteBufferCache cache) {
        this.context = context;
        this.cipherSuites = cipherSuites;
        this.cache = cache;
    }
    
    /**
     * Performs a test to ensure that SSLEngines can be created, handshaked, and
     * transfer data.
     * @return true if the test is successful, false if it isn't.
     */
    public boolean go() {
        try {
            goImpl();
            return true;
        } catch(Throwable t) {
            LOG.error("Error in TLS!", t);
            lastFailureCause = t;
            
            return false;
        }
    }
    
    /** Returns the last Throwable that caused this to fail. */
    public Throwable getLastFailureCause() {
        return lastFailureCause;
    }
    
    /** The actual implementation of the test. */
    private void goImpl() throws SSLException {
        SSLEngine server = context.createSSLEngine();
        SSLEngine client = context.createSSLEngine();
        
        server.setEnabledCipherSuites(cipherSuites);
        client.setEnabledCipherSuites(cipherSuites);
        server.setUseClientMode(false);
        client.setUseClientMode(true);
        server.setNeedClientAuth(false);
        server.setWantClientAuth(false);
        
        SSLSession session = server.getSession();        
        ByteBuffer clientOut = cache.getHeap(session.getPacketBufferSize());
        ByteBuffer serverOut = cache.getHeap(session.getPacketBufferSize());        

        if(LOG.isDebugEnabled())
            LOG.debug("Starting handshake loop.\nServer: " + server + "\nClient: " + client);
        
        try {
            doHandshake(client, server, clientOut, serverOut);
            doData(client, server, clientOut, serverOut);
        } finally {
            cache.release(clientOut);
            cache.release(serverOut);
        }
    }
    
    /** Handshakes between the client & server. */
    private void doHandshake(SSLEngine client, SSLEngine server, ByteBuffer clientOut, ByteBuffer serverOut) throws SSLException {
        SSLEngineResult clientResult = new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, 0);
        SSLEngineResult serverResult = new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_UNWRAP, 0, 0);

        while(true) {
            LOG.debug("Processing client handshake loop");
            serverOut.flip();
            clientResult = handshakeLoop(clientResult, client, serverOut, clientOut);
            serverOut.compact();
            
            LOG.debug("Processing server handshake loop");
            clientOut.flip();
            serverResult = handshakeLoop(serverResult, server, clientOut, serverOut);
            clientOut.compact();
            
            // If both are finished / not_handshaking, then exit.
            HandshakeStatus ch = clientResult.getHandshakeStatus();
            HandshakeStatus sh = serverResult.getHandshakeStatus();
            if( (ch == HandshakeStatus.FINISHED || ch == HandshakeStatus.NOT_HANDSHAKING) &&
                (sh == HandshakeStatus.FINISHED || sh == HandshakeStatus.NOT_HANDSHAKING)) {
                break;
            }
        }
    }
    
    /**
     * Loops in handshaking until an unwrap is required and no data is available
     * to unwrap, or a wrap is required by the destination buffer already has
     * some prewrapped data.
     */
    private SSLEngineResult handshakeLoop(SSLEngineResult result, SSLEngine engine, ByteBuffer src, ByteBuffer dst) throws SSLException {
        while(true) {
            if(LOG.isDebugEnabled())
                LOG.debug("Processing result: " + result + ", from engine: " + engine + ", src: " + src + ", dst: " + dst);
            
            if(result.getStatus() != SSLEngineResult.Status.OK)
                throw new IllegalStateException("Invalid result status: " + result);
            
            switch(result.getHandshakeStatus()) {
            case FINISHED:
            case NOT_HANDSHAKING:
                return result;
            case NEED_UNWRAP:
                // Nothing is left to unwrap, let the other side wrap.
                if(!src.hasRemaining())
                    return result;
                result = engine.unwrap(src, dst);
                break;
            case NEED_TASK:
                Runnable runner = engine.getDelegatedTask();
                runner.run();
                result = new SSLEngineResult(SSLEngineResult.Status.OK, engine.getHandshakeStatus(), 0, 0);
                break;
            case NEED_WRAP:
                // Need to let the other side unwrap if we already wrote data.
                if(dst.position() != 0)
                    return result;
                result = engine.wrap(BufferUtils.getEmptyBuffer(), dst);
                break;
            }
        }
    }
    
    /** Transfers data from the client -> server, and server -> client. */
    private void doData(SSLEngine client, SSLEngine server, ByteBuffer clientOut, ByteBuffer serverOut) throws SSLException {
        LOG.debug("Doing client -> server data test");
        doDataTest(new byte[] { 'C', 'L', 'I', 'E', 'N', 'T', ' ', 'T', 'E', 'S', 'T', ' ', 'O', 'U', 'T' } , 
								client, server, clientOut, serverOut);
        LOG.debug("Doing server -> client data test");
        doDataTest(new byte[] { 'S', 'E', 'R', 'V', 'E', 'R', ' ', 'T', 'E', 'S', 'T', ' ', 'O', 'U', 'T' }, server, client, serverOut, clientOut);
        LOG.debug("Finished data test");
    }
    
    /** Transfers a testString from srcEngine to dstEngine, using the buffers as scratch space. */
    private void doDataTest(byte[] testData, SSLEngine srcEngine, SSLEngine dstEngine, ByteBuffer writeBuf, ByteBuffer readBuf) throws SSLException {
        ByteBuffer data = ByteBuffer.wrap(testData);
        SSLEngineResult result = srcEngine.wrap(data, writeBuf);
        if(result.getStatus() != Status.OK)
            throw new IllegalStateException("Can't wrap data: " + result);
        if(data.hasRemaining())
            throw new IllegalStateException("Didn't wrap all data (" + testData + "): " + data);
        
        writeBuf.flip();
        result = dstEngine.unwrap(writeBuf, readBuf);
        if(result.getStatus() != Status.OK)
            throw new IllegalStateException("Can't unwrap data: " + result);
        if(writeBuf.hasRemaining())
            throw new IllegalStateException("Didn't unwrap all data!  readIn: " + writeBuf + ", made: " + readBuf);
        byte[] read = Arrays.copyOf(readBuf.array(), readBuf.position());
        if (!Arrays.equals(testData, read))
            throw new IllegalStateException("Wrong data read!  Wanted: " + Arrays.asList(testData) + ", was: " + Arrays.asList(read));
        
        readBuf.clear();
        writeBuf.clear();
    }
    
    /**
     * Determines if the error is ignorable.
     */
    public boolean isIgnorable(Throwable t) {
        // See CORE-286
        if(causeIs(t, NoSuchAlgorithmException.class))
            return true;

        // See CORE-286
        if(t instanceof NoSuchMethodError)
            return true;
        
        // See CORE-253
        if(t instanceof NoClassDefFoundError)
            return true;
        
        String msg = t.getMessage();
        
        // See CORE-253
        if(msg != null && msg.contains("Cipher buffering error"))
            return true;
        
        // We might make everything ignorable, but for now we want to see them.
        return false;
    }
    
    private boolean causeIs(Throwable t, Class<? extends Throwable> causeClass) {
        while(t != null) {
            if(causeClass.isAssignableFrom(t.getClass()))
                return true;
            t = t.getCause();
        }
        return false;
    }

}
