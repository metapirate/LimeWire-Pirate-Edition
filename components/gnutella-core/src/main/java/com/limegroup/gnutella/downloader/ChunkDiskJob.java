package com.limegroup.gnutella.downloader;

/** A job to be performed on the disk, using a chunk. */
public abstract class ChunkDiskJob {
    
    private final byte[] buf;
    
    public ChunkDiskJob(byte[] buf) {
        this.buf = buf;
    }
    
    /** Retrieves the chunk. */
    byte[] getChunk() {
        return buf;
    }
    
    /** Runs the actual job. */
    abstract void runChunkJob(byte[] buf);
    
    /** Runs any cleanup code. */
    abstract void finish();
    
}
