package org.limewire.nio;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.IntHashMap;

/**
 * Provides a <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/nio/ByteBuffer.html#direct">
 * non-direct</a> cache of {@link ByteBuffer ByteBuffers}. 
 *
 */
public class HeapByteBufferCache {

    // Store up to 1MB of byte[] here.
    private static final int MAX_SIZE = 1024 * 1024;
    
    private final IntHashMap<List<ByteBuffer>> CACHE = new IntHashMap<List<ByteBuffer>>();
        
    /** The total size of bytes stored in cache. */
    private long totalCacheSize;

    public ByteBuffer get() {
        return get(8192);
    }
    
    public synchronized ByteBuffer get(int size) {
        // trivial case - cache is empty
        if (CACHE.isEmpty()) { 
            ByteBuffer buf = ByteBuffer.allocate(size);
            return buf;
        }
        
        // if not, see if we have a buffer of the exact size
        List<ByteBuffer> l = CACHE.get(size);
        // if yes, return it.
        if (l != null && !l.isEmpty()) {
            ByteBuffer buf = l.remove(l.size() -1);
            totalCacheSize -= buf.capacity();
            return buf;
        } else {
            return ByteBuffer.allocate(size);
        }
    }

    public synchronized void put(ByteBuffer toReturn) {
        if(totalCacheSize > MAX_SIZE)
            return;
        
        int size = toReturn.capacity();
        toReturn.clear();
        List<ByteBuffer> l = CACHE.get(size);
        if (l == null) { 
            l = new ArrayList<ByteBuffer>(1);
            CACHE.put(size, l);
        }
        l.add(toReturn);
        totalCacheSize += toReturn.capacity();
    }
    
    public synchronized void clear() {
        CACHE.clear();
        totalCacheSize = 0;
    }
    
    public synchronized long getByteSize() {
        return totalCacheSize;
    }
}