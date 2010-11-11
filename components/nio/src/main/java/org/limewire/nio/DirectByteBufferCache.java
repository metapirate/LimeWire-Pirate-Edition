package org.limewire.nio;

import java.nio.ByteBuffer;
import java.util.Stack;
/**
 * Provides a <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/nio/ByteBuffer.html#direct">direct</a>
 * cache of {@link ByteBuffer ByteBuffers}.
 */
public class DirectByteBufferCache {

    private final Stack<ByteBuffer> CACHE = new Stack<ByteBuffer>();

    public ByteBuffer get() {
        synchronized (CACHE) {
            if (CACHE.isEmpty()) {
                ByteBuffer buf = ByteBuffer.allocateDirect(8192);
                return buf;
            } else {
                return CACHE.pop();
            }
        }
    }

    public void put(ByteBuffer buf) {
        buf.clear();
        CACHE.push(buf);
    }
    
    public void clear() {
        CACHE.clear();
    }
    
}
