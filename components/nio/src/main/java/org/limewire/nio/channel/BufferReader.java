package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.util.BufferUtils;

/** A <code>ReadableByteChannel</code> that reads directly from a buffer.  */
public class BufferReader implements InterestScatteringByteChannel {
    
    private final ByteBuffer buffer;
    
    public BufferReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void interestRead(boolean status) {
    }

    public int read(ByteBuffer dst) throws IOException {
        return BufferUtils.transfer(buffer, dst, false);
    }

    public long read(ByteBuffer[] dsts) throws IOException {
        return BufferUtils.transfer(buffer, dsts, 0, dsts.length, false);
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return BufferUtils.transfer(buffer, dsts, offset, length, false);
    }

    public void close() {
    }

    public boolean isOpen() {
        return true;
    }
    
}
