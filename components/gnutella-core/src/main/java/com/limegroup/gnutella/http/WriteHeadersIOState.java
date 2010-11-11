package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.limewire.nio.statemachine.WriteState;

public abstract class WriteHeadersIOState extends WriteState {
    /** The outgoing buffer, if we've made it.  (Null if we haven't.) */
    private ByteBuffer outgoing;
    /** The amount total that was written. */
    private long amountWritten;

    /**
     * Writes output to the channel.  This farms out the creation of the output
     * to the abstract method createOutgoingData().  That method will only be called once
     * to get the initial outgoing data.  Once all data has been written, the abstract
     * processWrittenHeaders() method will be called, so that subclasses can act upon
     * what they've just written.
     * <p>
     * This will return true if it needs to be called again to continue writing.
     * If it returns false, all data has been written and you can proceed to the next state.
     */
    @Override
    protected boolean processWrite(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
        if(outgoing == null) {
            outgoing = createOutgoingData();
        }
        
        int written = channel.write(outgoing);
        amountWritten += written;
        
        if(!outgoing.hasRemaining()) {
            processWrittenHeaders();
            return false;
        } else {
            return true;
        }
    }
    
    public final long getAmountProcessed() {
        return amountWritten;
    }
    
    /** Returns a ByteBuffer of data to write. */
    protected abstract ByteBuffer createOutgoingData() throws IOException;
    
    /** Processes the headers we wrote, after writing them.  May throw IOException if we need to disco. */
    protected abstract void processWrittenHeaders() throws IOException;
}
