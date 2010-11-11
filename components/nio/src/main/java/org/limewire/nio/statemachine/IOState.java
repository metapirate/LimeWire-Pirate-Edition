package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/** Defines the interface for all the states of asynchronous input/output. */
public interface IOState {
   
    /** Determines if this state is for writing. */
    boolean isWriting();
    
    /** Determines if this state is for reading. */
    boolean isReading();
    
    /**
     * Processes this state. If this returns <code>true</code>, the state requires 
     * further processing. This should be called repeatedly until it returns 
     * <code>false</code>, at which point the next state should be used.
     * <p>
     * The given <code>Channel</code> must be a <code>ReadableByteChannel</code> 
     * if it is for reading or a <code>WritableByteChannel</code> if it is for 
     * writing.
     * <p>
     * The given <code>ByteBuffer</code> should be used as scratch space for reading.
     */
    boolean process(Channel channel, ByteBuffer buffer) throws IOException;
    
    /**
     * Returns the amount of data that has been processed by this <code>IOState</code>.
     * This operation is optional; it should return -1 if unsupported.
     */
    long getAmountProcessed();
}
