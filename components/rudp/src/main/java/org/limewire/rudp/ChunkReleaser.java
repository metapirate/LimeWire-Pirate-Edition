package org.limewire.rudp;

import java.nio.ByteBuffer;

/**
 * Defines the interface to release a <code>ByteBuffer</code>.
 */
public interface ChunkReleaser {

    public void releaseChunk(ByteBuffer chunk);
}
