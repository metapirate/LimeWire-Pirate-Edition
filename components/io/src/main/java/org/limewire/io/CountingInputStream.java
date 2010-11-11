package org.limewire.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Counts the number of bytes successfully read or skipped.
 */
public final class CountingInputStream extends FilterInputStream {
    
    private int _count = 0;
    
    public CountingInputStream (final InputStream in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read != -1) {
            _count++;
        }
        return read;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read;
        try {
            read = super.read(b, off, len);
        } catch(ArrayIndexOutOfBoundsException aioob) {
            // happens.
            throw new IOException();
        }
        if (read != -1) {
            _count += read;
        }
        return read;
    }
    
    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        _count += (int)skipped;
        return skipped;
    }
    
    @Override
    public void close() throws IOException {
        in.close();
    }
    
    public int getAmountRead() {
        return _count;
    }
    
    public void clearAmountRead() {
        _count = 0;
    }
    
    
} // class
