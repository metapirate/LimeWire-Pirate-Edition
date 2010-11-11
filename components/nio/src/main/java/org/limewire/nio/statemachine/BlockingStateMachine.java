package org.limewire.nio.statemachine;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.FileUtils;

/**
 * Blocks until the state machine finishes; use <code>BlockingStateMachine</code> 
 * as an alternative to {@link IOStateMachine}. 
 * <p>
 * <code>BlockingStateMachine</code> is useful if you want to use the same state
 * code from an NIO portion where only <code>OutputStreams</code> and 
 * <code>InputStreams</code> are available.
 */
public class BlockingStateMachine implements Closeable, Shutdownable {
    
    private static final Log LOG = LogFactory.getLog(BlockingStateMachine.class);

    /** The states this will use while handshaking.*/
    private final List<IOState> states;
    /** A reading channel wrapping an InputStream. */
    private final ReadableByteChannel readChannel;
    /** A writing channel wrapping an OutputStream. */
    private final WritableByteChannel writeChannel;
    /** The ByteBuffer to use for reading. */
    private final ByteBuffer readBuffer;
    
    public BlockingStateMachine(List<IOState> states, InputStream in, OutputStream out) {
        this(states, 2048, in, out);
    }

    public BlockingStateMachine(List<IOState> states, int bufferSize, InputStream in, OutputStream out) {
        this.states = states;
        this.readBuffer = NIODispatcher.instance().getBufferCache().getHeap(bufferSize);
        this.readChannel = Channels.newChannel(in);
        this.writeChannel = Channels.newChannel(out);
    }
    
    /**
     * Adds a new state to process.
     */
    public void addState(final IOState newState) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding single state: " + newState);
        states.add(newState);
    }
    
    /** Adds a collection of new states to process. */
    public void addStates(final List<? extends IOState> newStates) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding multiple states: " + newStates);
        states.addAll(newStates);        
    }
    
    /** Adds an array of new states to process. */
    public void addStates(final IOState... newStates) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding multiple states...");
        for(int i = 0; i < newStates.length; i++) {
            if(LOG.isDebugEnabled())
                LOG.debug(" state[" + i + "]: " + newStates[i]);
            states.add(newStates[i]);
        }        
    }
    
    /** Aborts this statemachine. */
    public void close() {
        FileUtils.close(readChannel);
        FileUtils.close(writeChannel);
    }
    
    /** Releases any data the statemachine retained. */
    public void shutdown() {
        NIODispatcher.instance().getBufferCache().release(readBuffer);
    }
    
    /** Begins processing through the states. Returns only when every state is processed. */
    public void process() throws IOException {
        for(Iterator<IOState> i = states.iterator(); i.hasNext(); ) {
            IOState current = i.next();
            if(current.isReading())
                current.process(readChannel, readBuffer);
            else if(current.isWriting())
                current.process(writeChannel, null);
            else
                throw new IllegalStateException("expected reading | writing state");
            i.remove();
        }
    }
    
    
    
    
    
}
