package org.limewire.collection;

import java.util.Stack;

/**
 * A cache of byte[]. Sets a limit to the number of byte arrays that can be 
 * created. <code>ByteArrayCache</code> has methods to create and release the 
 * byte arrays, as well as check the size (total size, remaining size, and byte
 * array size).
 * <p>  
 * When the maximum number of byte arrays are created ({@link #getCreated()} ==
 * {@link #getMaxSize()}), attempts to get a new byte array ({@link #get()}) 
 * block until a byte array is freed ({@link #release(byte[])}).
<pre>
    ByteArrayCache bac = new ByteArrayCache(3, 2);

    byte[] b1 = bac.getQuick();
    byte[] b2 = bac.getQuick();
    
    for(int i = 0; i &lt; b1.length; i++)
        b1[i] = 1;
    
    System.arraycopy(b1, 0, b2, 0, b1.length);
    for(int i = 0; i < b2.length; i++)
        System.out.println("b2[" + i + "]: " + b2[i]);

    Output:
        b2[0]: 1
        b2[1]: 1
       
</pre>

 * 
 */
public class ByteArrayCache {
    
    /** Default # of byte[]'s to hold. */
    private static final int DEFAULT_SIZE = 512;
    
    /** Default length of returned bytes. */
    private static final int DEFAULT_LENGTH = 1024;

    /** Holds cached byte[]'s for future re-use. */
    private final Stack<byte[]> CACHE = new Stack<byte[]>();
    
    /** The total size of the bytes stored in the cache. */
    private int _totalSize;
    
    /** The number of byte[]'s this cache has created that haven't been erased. */
    private int _numCreated;

    /** The number of byte[]'s to create before blocking when the next one is gotten. */
    private final int _maxSize;
    
    /** The length of buffers to create. */
    private final int _length;

    /** Constructs a new ByteArraCache using the default size of 512 & length of 1024. */
    public ByteArrayCache() {
        this(DEFAULT_SIZE, DEFAULT_LENGTH);
    }
    
    /** Constructs a new ByteArrayCache using the given maxSize & length. */
    public ByteArrayCache(int maxSize, int length) {
        _maxSize = maxSize;
        _length = length;
        assert _length > 0 : "constructing cache of emtpy arrays "+_length;
    }
    
    /** Returns the size of all cached bytes. */
    public synchronized int getCacheSize() {
        return _totalSize;
    }
    
    /**
     * Attempts to retrieve a new byte[].
     * If the cache is empty and getCreated() is >= getMaxSize(), this will 
     * block until a byte[] is returned to the cache.
     */
    public synchronized byte[] get() throws InterruptedException {
        while(true) {
            if (!CACHE.isEmpty()) {
                byte[] b = CACHE.pop();
                _totalSize -= b.length;
                return b;
            } else if (_numCreated < _maxSize) {
                _numCreated++;
                return new byte[_length];
            } else {
                wait();
            }
        }
    }
    
    /**
     * Attempts to retrieve a new byte[]. If the cache is empty and getCreated()
     * is >= getMaxSize(), this will return null.
     */
    public synchronized byte[] getQuick() {
        if (!CACHE.isEmpty()) {
            byte[] b = CACHE.pop();
            _totalSize -= b.length;
            return b;
        } else if (_numCreated < _maxSize) {
            _numCreated++;
            return new byte[_length];
        } else {
            return null;
        }
    }
    
    /**
     * Returns a byte[] to this cache.
     * The byte[] MUST HAVE BEEN A byte[] RETURNED FROM get().
     */
    public synchronized void release(byte[] data) {
        assert data.length == _length : "illegal length: " + data.length;
        _totalSize += data.length;
        CACHE.ensureCapacity(_maxSize);
        CACHE.push(data);
        notifyAll();
    }
    
    /** Clears all items in the cache. */
    public synchronized void clear() {
        _numCreated -= CACHE.size();
        _totalSize = 0;
        CACHE.clear();
        notifyAll();
    }
    
    /** Determines if there's space in the cache for another byte[]. */
    public synchronized boolean isBufferAvailable() {
        return !CACHE.isEmpty() || _numCreated < _maxSize;
    }
    
    /** Returns the number of byte[]'s this cache has created. */
    public synchronized int getCreated() {
        return _numCreated;
    }
    
    /** Returns the length of the byte[]'s this creates. */
    public int getLength() {
        return _length;
    }
    
    /** Returns the maximum number of byte[]'s this will create. */
    public int getMaxSize() {
        return _maxSize;
    }
    
}
