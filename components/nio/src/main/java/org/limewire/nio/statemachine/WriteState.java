package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.WritableByteChannel;

/**
 * Abstract class that represents a write state and declares that reading
 * is not taking place.
 */
//TODO rename to AbstractWriteState - when rename with Abstract prepended, 
//remove "Abstract class that" from the javadoc comment.
public abstract class WriteState implements IOState {

    public final boolean isWriting() {
        return true;
    }

    public final boolean isReading() {
        return false;
    }

    public final boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        return processWrite((WritableByteChannel)channel, buffer);
    }
    
    protected abstract boolean processWrite(WritableByteChannel channel, ByteBuffer buffer) throws IOException;

}
