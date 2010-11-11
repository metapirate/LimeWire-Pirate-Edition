package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Process a write state. <code>SimpleWriteState</code> declares a write is 
 * taking place. Additionally, <code>SimpleWriteState</code> returns how much 
 * was written by the channel.
 */
public class SimpleWriteState extends WriteState {
    
    /** The outgoing buffer, if we've made it.  (Null if we haven't.) */
    private ByteBuffer outgoing;
    /** The amount total that was written. */
    private long amountWritten;

    /** Creates a new <code>SimpleWriteState</code> that will write the given data. */
    public SimpleWriteState(ByteBuffer outgoing) {
        this.outgoing = outgoing;
    }

    /**
     * Writes output to the channel.
     * <p>
     * This will return true if it needs to be called again to continue writing.
     * If it returns false, all data has been written and you can proceed to the next state.
     */
    @Override
    protected boolean processWrite(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
 
        int written = channel.write(outgoing);
        amountWritten += written;
        
        return outgoing.hasRemaining();
    }
    
    public final long getAmountProcessed() {
        return amountWritten;
    }
}