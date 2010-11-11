package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.limewire.util.BufferUtils;

/**
 * A state used for skipping over data.
 * <p>
 * If constructed with {@link #ReadSkipState(AtomicLong)}, it is possible to 
 * change the skipping value prior to the state's first processing. 
 * <p>
 * You MUST NOT change the skipping value after the state has begun processing.
 */
public class ReadSkipState extends ReadState {
    
    private final AtomicLong leftToRead;
    
    public ReadSkipState(long length) {
        this(new AtomicLong(length));
    }
    
    public ReadSkipState(AtomicLong length) {
        this.leftToRead = length;
    }
    
    @Override
    protected boolean processRead(ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
        leftToRead.set(BufferUtils.delete(buffer, leftToRead.get()));     
        int read = 0;
        while(leftToRead.get() > 0 && (read = rc.read(buffer)) > 0)
            leftToRead.set(BufferUtils.delete(buffer, leftToRead.get()));
        
        if(leftToRead.get() > 0 && read == -1)
            throw new IOException("EOF");
        else
            return leftToRead.get() > 0; // requires more processing if still stuff to read.
    }

    public long getAmountProcessed() {
        return -1;
    }

}
