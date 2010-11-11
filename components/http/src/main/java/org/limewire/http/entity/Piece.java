package org.limewire.http.entity;

import java.nio.ByteBuffer;

/**
 * Represents a chunk of bytes that is stored in a {@link ByteBuffer}.
 */
public class Piece implements Comparable<Piece> {

    private long offset;
    
    private ByteBuffer buffer;

    private int length;
    
    public Piece(long offset, ByteBuffer buffer) {
        this.offset = offset;
        this.buffer = buffer;
        this.length = buffer.limit();
    }

    /**
     * Returns the content stored in this piece.
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Returns the number of bytes of this piece.
     */
    public int getLength() {
        return length;
    }
    
    /**
     * Returns the offset of this piece. The meaning depends on the context. If
     * the piece was read from a file this could be the offset of the first byte
     * of this piece.
     */
    public long getOffset() {
        return offset;
    }    

    public int compareTo(Piece o) {
        long l = offset - o.offset;
        return (l < 0) ? -1 : (l == 0) ? 0 : 1;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[offset=" + offset + ",length=" + buffer.remaining() + "]";
    }
    
}
