package org.limewire.player.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * An InputStream created from a RandomAccessFile. Seek is overridden
 * to take advantage of the fast search time of RandomAccessFile.
 */
public class RandomAudioInputStream extends InputStream {

    /**
     * The file the input stream reads from.
     */
    private final RandomAccessFile file;

    /**
     * The current byte location in the file.
     */
    private long fileBytePosition;

    public RandomAudioInputStream(RandomAccessFile file) {
        this.file = file;
        fileBytePosition = 0;
    }

    @Override
    public int read() throws IOException {
        int a = file.read();
        fileBytePosition++;
        return a;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (file == null) {
            return -1;
        }
        int r = file.read(b, off, len);
        fileBytePosition += r;
        return r;
    }

    @Override
    public int available() throws IOException {
        return (int) (file.length() - fileBytePosition);
    }

    /**
     * Skip uses the RandomAccessFile seek. Note that unlike normal
     * seek on an inputStream, the value passed in here is the location
     * from the beginning of the file, not the current location in the stream.
     */
    @Override
    public long skip(long bytesToSkip) throws IOException { 
        int bytes = file.skipBytes((int)bytesToSkip);
        fileBytePosition = bytes;
        return bytes;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
