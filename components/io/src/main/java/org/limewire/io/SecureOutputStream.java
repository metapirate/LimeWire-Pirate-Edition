package org.limewire.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * Writes in periodic intervals a checksum of the written bytes to the output 
 * stream. {@link SecureInputStream} can verify whether or not the bytes are 
 * still valid.
 */
public class SecureOutputStream extends FilterOutputStream {
    
    private final MessageDigest md;
    
    private final int dataSize;
    
    private int count = 0;
    
    private boolean open = true;
    
    public SecureOutputStream(OutputStream out) throws IOException {
        this(out, new CRC32MessageDigest(), 512);
    }
    
    public SecureOutputStream(OutputStream out, MessageDigest md) throws IOException {
        this(out, md, 512);
    }
    
    public SecureOutputStream(OutputStream out, int blockSize) throws IOException {
        this(out, new CRC32MessageDigest(), blockSize);
    }
    
    public SecureOutputStream(OutputStream out, MessageDigest md, int blockSize) throws IOException {
        super(out);
        
        if (md == null) {
            throw new NullPointerException("MessageDigest is null");
        }
        
        // Make sure the digest length is defined
        int digestLength = md.getDigestLength();
        if (digestLength == 0) {
            throw new IllegalArgumentException("Digest length is undefined");
        }
        
        // Block size must be greater than zero
        if (blockSize <= 0) {
            throw new IllegalArgumentException("Illegal block size: " + blockSize);
        }
        
        // Block size must be greater than digest length
        if (blockSize <= digestLength) {
            throw new IllegalArgumentException("Block size must be greater than digest length: " 
                    + blockSize + " <= " + digestLength);
        }
        
        // Create the header
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(md.getAlgorithm());
        dos.writeInt(md.getDigestLength());
        dos.writeInt(blockSize);
        dos.close();
        
        byte[] header = baos.toByteArray();
        
        // Write the length of the header
        out.write((header.length >> 24) & 0xFF);
        out.write((header.length >> 16) & 0xFF);
        out.write((header.length >>  8) & 0xFF);
        out.write((header.length      ) & 0xFF);
        
        // Write the header
        out.write(header, 0, header.length);
        
        // Write the digest of the header
        md.update(header, 0, header.length);
        byte[] digest = md.digest();
        out.write(digest, 0, digest.length);
        
        md.reset();
        
        this.md = md;
        
        // [data] + [checksum] == blockSize
        this.dataSize = blockSize - digestLength;
    }
    
    /**
     * Returns the block (buffer) size of the stream.
     */
    public int getBlockSize() {
        return dataSize + md.getDigestLength();
    }
    
    /**
     * Returns the MessageDigest.
     */
    public MessageDigest getMessageDigest() {
        return md;
    }
    
    private void digest() throws IOException {
        byte[] digest = md.digest();
        out.write(digest, 0, digest.length);
        md.reset();
    }
    
    @Override
    public void write(int b) throws IOException {
        if (!open) {
            throw new IOException("Stream is closed");
        }
        
        super.write(b);
        
        md.update((byte)(b & 0xFF));
        count++;
        
        if (count % dataSize == 0) {
            digest();
        }
    }
    
    @Override
    public void close() throws IOException {
        if (open) {
            open = false;
            
            // Write the digest for the last bytes
            if (count % dataSize != 0) {
                digest();
            }
        }
        
        super.close();
    }
}
