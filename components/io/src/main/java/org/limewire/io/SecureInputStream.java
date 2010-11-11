package org.limewire.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.security.MessageDigest;

/**
 * Reads a stream of bytes written by a {@link SecureOutputStream} and checks
 * if the bytes are still valid. <code>SecureInputStream</code> throws 
 * exceptions upon problems reading the <code>SecureInputStream</code>.
 */
public class SecureInputStream extends FilterInputStream {
    
    private final MessageDigest md;
    
    private final byte[] buffer;
    
    private int pos = 0;
    
    private int length = -1;
    
    public SecureInputStream(InputStream in) throws IOException {
        this(in, new CRC32MessageDigest());
    }
    
    public SecureInputStream(InputStream in, MessageDigest md) throws IOException {
        super(in);
        
        if (md == null) {
            throw new NullPointerException("MessageDigest is null");
        }
        
        // Read the length of the header
        int length = 0;
        for (int i = 0; i < 4; i++) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("Couldn't read the length of the header");
            }
            length = (length << 8) | (b & 0xFF);
        }
        
        // A simple sanity check. The length cannot be negative 
        // nor greater than X (4+4+4+name of algorithm).
        if (length <= 0 || length >= 512) {
            throw new StreamCorruptedException("Invalid length of the header: " + length);
        }
        
        // Read the actual header
        byte[] header = new byte[length];
        for (int i = 0; i < header.length; i++) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("Couldn't read the header");
            }
            
            header[i] = (byte)(b & 0xFF);
        }
        md.update(header, 0, header.length);
        byte[] actual = md.digest();
        
        // Read the expected checksum and compare it on the fly
        for (int i = 0; i < actual.length; i++) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("Couldn't read the checksum of length " + actual.length);
            }
            
            if (actual[i] != (byte)(b & 0xFF)) {
                throw new StreamCorruptedException("Header checksums do not match");
            }
        }
        
        String algorithm = null;
        int digestLength = 0;
        int blockSize = 0;
        
        // Get the fields from the header
        ByteArrayInputStream bias = new ByteArrayInputStream(header);
        DataInputStream dis = new DataInputStream(bias);
        algorithm = dis.readUTF();
        digestLength = dis.readInt();
        blockSize = dis.readInt();
        dis.close();
        
        // Make some final sanity checks
        if (!algorithm.equals(md.getAlgorithm())) {
            throw new StreamCorruptedException("Expected a MessageDigest of type " 
                    + algorithm + " but is " + md.getAlgorithm());
        }
        
        if (digestLength != md.getDigestLength()) {
            throw new StreamCorruptedException("Expected a MessageDigest with length " 
                    + digestLength + " but is " + md.getDigestLength());
        }
        
        md.reset();
        
        this.md = md;
        this.buffer = new byte[blockSize];
    }
    
    /**
     * Returns the block (buffer) size of the stream.
     */
    public int getBlockSize() {
        return buffer.length;
    }
    
    /**
     * Returns the MessageDigest.
     */
    public MessageDigest getMessageDigest() {
        return md;
    }
    
    private int refill() throws IOException {
        assert (pos >= length);
        
        // Reset everything
        pos = 0;
        length = 0;
        md.reset();
        
        // Fill the buffer
        while(length < buffer.length) {
            int r = in.read();
            if (r < 0) {
                break;
            }
            
            buffer[length++] = (byte)(r & 0xFF);
        }
        
        // Is EOF?
        if (length == 0) {
            return -1;
        }
        
        // Get the actual length of the payload
        int digestLength = md.getDigestLength();
        length -= digestLength; 
        
        if (length <= 0) {
            throw new StreamCorruptedException("Illegal payload length: " + length);
        }
        
        // Compute the hash
        md.update(buffer, 0, length);
        
        // Compare the actual hash with the expected hash
        byte[] digest = md.digest();
        assert (digest.length == digestLength);
        for (int i = 0; i < digest.length; i++) {
            if (digest[i] != buffer[length+i]) {
                throw new StreamCorruptedException("Checksums do not match");
            }
        }
        
        return length;
    }

    @Override
    public int read() throws IOException {
        if (pos >= length) {
            refill();
            if (pos >= length) {
                return -1;
            }
        }
        
        return buffer[pos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                   ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        
        int total = 0;
        while(total < len) {
            if (pos >= length) {
                refill();
                if (pos >= length) {
                    // EOF
                    break;
                }
            }
            
            int copy = Math.min(length-pos, len-total);
            System.arraycopy(buffer, pos, b, off+total, copy);
            pos += copy;
            total += copy;
        }
        
        return (total > 0 ? total : -1);
    }
}
