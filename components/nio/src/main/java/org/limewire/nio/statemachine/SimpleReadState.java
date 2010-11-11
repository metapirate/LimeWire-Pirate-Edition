package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * An abstract class reading state that reads all its data first, then validates
 * the data at the end. <code>SimpleReadState</code> also clears all data it 
 * reads from the <code>ByteBuffer buffer</code> after the buffer is filled and
 * validated.
 */
//TODO rename to AbstractValidatingReadState
public abstract class SimpleReadState extends ReadState {
    /** The amount of data we expected to read. */
    private final int expect;
    /** The total amount of data we've read. */
    private int amountRead = 0;
    /** The ByteBuffer we'll be using. */
    private ByteBuffer buffer;
    
    public SimpleReadState(int expect) {
        this.expect = expect;
    }

    @Override
    protected boolean processRead(ReadableByteChannel channel, ByteBuffer scratchBuffer) throws IOException {
        if(buffer == null) {
            buffer = scratchBuffer.slice();
            buffer.limit(expect);
        }
        
        int read = 0;
        while(buffer.hasRemaining() && (read = channel.read(buffer)) > 0)
            amountRead += read;
        
        if(buffer.hasRemaining() && read == -1)
            throw new IOException("EOF");
        
        // if finished filling the buffer...
        if(!buffer.hasRemaining()) {
            validateBuffer(buffer);
            buffer.clear();
            return false;
        }
        
        // still need to read more
        return true;
    }

    public long getAmountProcessed() {
        return amountRead;
    }
    
    /** 
     * Validates the buffer, after it is filled. Throws an 
     * <code>IOException</code> if there are validation errors. For example, 
     * you can check <code>buffer.get(0)</code> is the proper version, or even 
     * <code>buffer.get(1)</code> contains the proper status.
     */
    public abstract void validateBuffer(ByteBuffer buffer) throws IOException;

}
