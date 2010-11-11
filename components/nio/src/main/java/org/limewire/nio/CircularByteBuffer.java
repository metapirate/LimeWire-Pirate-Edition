package org.limewire.nio;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.limewire.service.ErrorService;

/**
 * Allows to read and write to/from channels and other buffers
 * with virtually no memory overhead.
 */
public class CircularByteBuffer {

    private static final DevNull DEV_NULL = new DevNull();

    private final ByteBufferCache cache;

    private final int capacity;

    private ByteBuffer in, out;

    private ByteOrder order = ByteOrder.BIG_ENDIAN;

    private boolean lastOut = true;

    public CircularByteBuffer(int capacity, ByteBufferCache cache) {
        this.cache = cache;
        this.capacity = capacity;
    }

    private void initBuffers() {
        if (in == null) {
            assert out == null;
            in = cache.getHeap(capacity);
            out = in.asReadOnlyBuffer();
        } else
            assert out != null;
    }

    public final int remainingIn() {
        if (in == null)
            return capacity;

        int i = in.position();
        int o = out.position();
        if (i > o)
            return in.capacity() - i + o;
        else if (i < o)
            return o - i;
        else
        	return lastOut ? in.capacity() : 0;
    }
    
    public final int remainingOut() {
        return capacity() - remainingIn();
    }

    public void put(ByteBuffer src) {
        if (src.remaining() > remainingIn())
            throw new BufferOverflowException();
        
        if (src.hasRemaining())
            lastOut = false;
        else 
            return;
        
        initBuffers();
        if (src.remaining() > in.remaining()) {
            int oldLimit = src.limit();
            src.limit(src.position() + in.remaining());
            in.put(src);
            in.rewind();
            src.limit(oldLimit);
        }
        
        in.put(src);
    }
    
    public void put(CircularByteBuffer src) {
        if (src.remainingOut() > remainingIn())
            throw new BufferOverflowException();
        
        initBuffers();
        if (in.remaining() < src.remainingOut()) {
            src.out.limit(in.remaining());
            in.put(src.out);
            in.rewind();
            src.out.limit(src.out.capacity());
        }
        
        in.put(src.out);
        lastOut = false;
    }
    
    public byte get() {
        if (remainingOut() < 1)
            throw new BufferUnderflowException();
        if (!out.hasRemaining())
            out.rewind();
        byte ret = out.get();
        releaseIfEmpty();
        lastOut = true;
        return ret;
    }
    
    public void get(byte [] dest) {
        get(dest,0,dest.length);
    }
    
    public void get(byte [] dest, int offset, int length) {
        if (remainingOut() < length)
            throw new BufferUnderflowException();
        
        if (length > 0)
            lastOut = true;
        else
            return;
        
        if (out.remaining() < length) {
            int remaining = out.remaining();
            out.get(dest, offset, remaining);
            offset+=remaining;
            length-=remaining;
            out.rewind();
        }
        
        out.get(dest,offset,length);
        releaseIfEmpty();
    }
    
    public void get(ByteBuffer dest) {
        if (remainingOut() < dest.remaining())
            throw new BufferUnderflowException();
        
        if (dest.remaining() > 0)
            lastOut = true;
        else
            return;
        
        if (out.remaining() < dest.remaining()) { 
            dest.put(out);
            out.rewind();
        }
        out.limit(out.position() + dest.remaining());
        dest.put(out);
        out.limit(out.capacity());
        releaseIfEmpty();
    }
    
    private void releaseIfEmpty() {
        if (in != null && out != null && out.position() == in.position()) {
            cache.release(in);
            in = null;
            out = null;
        }
    }
    
    public int write(WritableByteChannel sink, int len) throws IOException {
        int written = 0;
        int thisTime = 0;
        while (remainingOut() > 0 && written < len) {
            if (!out.hasRemaining())
                out.rewind();
            if (in.position() > out.position()) {
                if (len == Integer.MAX_VALUE)
                    out.limit(in.position());
                else
                    out.limit(Math.min(in.position(), len - written + out.position()));
            }
            try {
                thisTime = sink.write(out);
            } finally {
                if (thisTime > 0)
                    lastOut = true;
            }
            
            out.limit(out.capacity());
            if (thisTime == 0)
                break;
            
            written += thisTime;
        }
        releaseIfEmpty();
        return written;
    }
    
    public int write(WritableByteChannel sink) throws IOException {
        return write(sink, Integer.MAX_VALUE);
    }
    
    /**
     * Reads data from the source channel.
     * @return the amount of data read, >= 0
     * @throws IOException if an error occurred or 
     * no data was read and end of stream was reached.
     */
    public int read(ReadableByteChannel source) throws IOException {
        int read = 0;
        int thisTime = 0;
        
        while (remainingIn() > 0) {
        	initBuffers();
            if (!in.hasRemaining())
                in.rewind();
            if (out.position() > in.position()) 
                in.limit(out.position());
            try {
                thisTime = source.read(in);
            } finally {
                if (thisTime > 0)
                    lastOut = false;
            }
            
            in.limit(in.capacity());
            if (thisTime == 0)
                break;
            if (thisTime == -1) {
                if (read == 0)
                    throw new IOException();
                return read;
            }
            
            read += thisTime;
        } 
        
        return read;
    }
    
    public int size() {
    	return remainingOut();
    }
    
    public int capacity() {
        return capacity;
    }
    
    @Override
    public String toString() {
        return "circular buffer in:"+in+" out:"+out;
    }
    
    public void order(ByteOrder order) {
        this.order = order;
    }
    
    public int getInt() throws BufferUnderflowException {
        if (remainingOut() < 4)
            throw new BufferUnderflowException();

        if (order == ByteOrder.BIG_ENDIAN)
            return getU() << 24 | getU() << 16 | getU() << 8 | getU();
        else
            return getU() | getU() << 8 | getU() << 16 | getU() << 24;
    }

    private int getU() {
        return get() & 0xFF;
    }

    public void discard(int num) {
        if (remainingOut() < num)
            throw new BufferUnderflowException();
        try {
            write(DEV_NULL, num);
        } catch (IOException impossible) {
            ErrorService.error(impossible);
        }
    }

    private static class DevNull implements WritableByteChannel {

        public int write(ByteBuffer src) throws IOException {
            int ret = src.remaining();
            src.position(src.limit());
            return ret;
        }

        public void close() throws IOException {
        }

        public boolean isOpen() {
            return true;
        }

    }
}
