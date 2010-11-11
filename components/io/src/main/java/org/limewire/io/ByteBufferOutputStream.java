package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Wraps {@link ByteBuffer ByteBuffers} so it can be accessed as an
 * {@link OutputStream}. This class exposes many methods to make using 
 * <code>ByteBuffer</code> more convenient. <code>ByteBufferOutputStream</code>
 * is similar to {@link ByteArrayInputStream}, however it uses 
 * <code>ByteBuffer</code>. 
 * <p>
 * <code>ByteBufferOutputStream</code> can optionally grow to 
 * meet the needed size. If the optional growth setting is off (default), yet 
 * growth is needed, then {@link BufferOverflowException} is thrown.
 */
public class ByteBufferOutputStream extends OutputStream {
    
    /** The backing ByteBuffer. If growth is enabled, the buffer may change. */
    protected ByteBuffer buffer;
    
    /** Whether or not this can grow. */
    protected boolean grow;
    
    /**
     * Creates an OutputStream initially sized at 32 that can grow.
     */
    public ByteBufferOutputStream() {
        this(32);
    }

    /**
     * Creates an OutputStream of the given size that can grow.
     */
    public ByteBufferOutputStream(int size) {
        this(ByteBuffer.allocate(size), true);
    }
    
    /**
     * Creates an OutputStream of the given size that can grow as needed.
     */
    public ByteBufferOutputStream(int size, boolean grow) {
        this(ByteBuffer.allocate(size), grow);
    }
    
    /**
     * Creates an OutputStream with the given backing array that cannot grow.
     */
    public ByteBufferOutputStream(byte[] backing) {
        this(ByteBuffer.wrap(backing), false);
    }
    
    /**
     * Creates an OutputStream using the given backing array, starting at position
     * 'pos' and allowing writes for the given length. The stream cannot grow.
     */
    public ByteBufferOutputStream(byte[] backing, int pos, int length) {
        this(ByteBuffer.wrap(backing, pos, length), false);
    }
    
    /**
     * Creates an OutputStream backed by the given ByteBuffer. The stream cannot 
     * grow.
     */
    public ByteBufferOutputStream(ByteBuffer backing) {
        this(backing, false);
    }
    
    /**
     * Creates an OutputStream backed by the given ByteBuffer. If 'grow' is true,
     * then the referenced ByteBuffer may change when the backing array is grown.
     */
    public ByteBufferOutputStream(ByteBuffer backing, boolean grow) {
        this.buffer = backing;
        this.grow = grow;
    }
    
    /** Does nothing. */
    @Override
    public void close() throws IOException {}
    
    /** Resets the data so that the backing buffer can be reused. */
    public void reset() {
        buffer.clear();
    }
    
    /** Returns the amount of data currently stored. */
    public int size() {
        return buffer.position();
    }
    
    /**
     * Returns a byte[] of the valid bytes written to this stream.
     * <p>
     * This _may_ return a reference to the backing array itself (but it is not
     * guaranteed to), so the ByteBufferOutputStream should not be used again
     * after this is called if you want to preserve the contents of the array.
     */
    public byte[] toByteArray() {
        byte[] arr = buffer.array();
        int offset = buffer.arrayOffset();
        int position = buffer.position();
        if(offset == 0 && position == arr.length)
            return arr; // no need to copy, the array is all filled up.
            
        byte[] out = new byte[position];
        System.arraycopy(arr, offset, out, 0, position);
        return out;
    }
    
    /**
     * Returns the backing buffer.
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }
    
    /**
     * Writes the current data to the given buffer.
     * If the sink cannot hold all the data stored in this buffer,
     * nothing is written and a <code>BufferOverflowException</code> is thrown.
     * All written bytes are cleared.
     */
    public void writeTo(ByteBuffer sink) {
        buffer.flip();
        sink.put(buffer);
        buffer.compact();
    }
    
    /**
     * Writes the current data to the given byte[].
     * If the data is larger than the byte[], nothing is written
     * and a <code>BufferOverflowException</code> is thrown.
     * All written bytes are cleared.
     */
    public void writeTo(byte[] out) {
        writeTo(out, 0, out.length);
    }
    
    /**
     * Writes the current data to the given byte[], starting at offset off and going
     * for length len. If the data is larger than the length, nothing is written and
     * a <code>BufferOverflowException</code> is thrown.
     * All written bytes are cleared.
     */
    public void writeTo(byte[] out, int off, int len) {
        buffer.flip();
        buffer.get(out, off, len);
        buffer.compact();
    }
    
    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the platform's default character encoding.
     */
    @Override
    public String toString() {
        return new String(buffer.array(), buffer.arrayOffset(), buffer.position());
    }
    
    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the specified character encoding.
     */
    public String toString(String encoding) throws UnsupportedEncodingException {
        return new String(buffer.array(), buffer.arrayOffset(), buffer.position(), encoding);
    }
    
    /** Grows the buffer to accommodate the given size. */
    private void grow(int len) {
        int size = buffer.capacity();
        int newSize = Math.max(size << 1, size + len);
        ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }
    
    /**
     * Writes the byte[] to the buffer, starting at off for len bytes.
     * If the buffer cannot grow and this exceeds the size of the buffer, a
     * <code>BufferOverflowException</code> is thrown and no data is written. 
     * If the buffer can grow, a new buffer is created & data is written.
     */
    @Override
    public void write(byte[] b, int off, int len) {
        if(grow && len > buffer.remaining())
            grow(len);
        
        buffer.put(b, off, len);
    }
    
    /**
     * Writes the remaining bytes of the <code>ByteBuffer</code> to the buffer.
     * If the buffer cannot grow and this exceeds the size of the buffer, a
     * <code>BufferOverflowException</code> is thrown and no data is written. 
     * If the buffer can grow, a new buffer is created & data is written.
     */
    public void write(ByteBuffer src) {
    	if (grow && src.remaining() > buffer.remaining())
    	    grow(src.remaining());
    	
    	buffer.put(src);
    }
    
    /**
     * Writes the given byte to the buffer.
     * If the buffer is already full and cannot grow, a 
     * <code>BufferOverflowException</code> is thrown
     * and no data is written. If the buffer can grow, a new buffer is created
     * and data is written.
     */
    @Override
    public void write(int b) {
        if(grow && !buffer.hasRemaining())
            grow(1);
            
        buffer.put((byte)b);
    }
    
    /**
     * Writes the buffer to the given <code>OutputStream</code>.
     * All written bytes are cleared.
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(buffer.array(), buffer.arrayOffset(), buffer.position());
        buffer.clear();
    }
}
