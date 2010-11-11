package com.limegroup.gnutella.downloader;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.ByteArrayCache;
import org.limewire.collection.PowerOf2ByteArrayCache;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ManagedThread;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.ServiceScheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/** Manages writing / reading from / to disk. */
@EagerSingleton
public class DiskController {
    
    private static final Log LOG = LogFactory.getLog(DiskController.class);

    /** The thread that does the actual verification & writing */
    private final ThreadPoolExecutor QUEUE = ExecutorsHelper.newSingleThreadExecutor(
            new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = new ManagedThread(r, "BlockingVF");
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY+1);
                    return t;
                }
            });
    
    /**
     * A list of DelayedWrites that will write when space becomes available in the cache.
     * LOCKING: Lock on the below CACHE.
     */
    private final List<DelayedWrite> DELAYED = new LinkedList<DelayedWrite>();   
    /**  A cache for byte[]s. */
    private final ByteArrayCache CACHE = new ByteArrayCache(512, HTTPDownloader.BUF_LENGTH);    
    /** a bunch of cached byte[]s for verifiable chunks */
    private final PowerOf2ByteArrayCache CHUNK_CACHE = new PowerOf2ByteArrayCache();
    /** The number of chunks scheduled to be written. */
    private int chunksScheduled = 0;
    /** A lock to use for the queue size + chunksScheduled. */
    private final Object SCHEDULE_LOCK = new Object();
    
    @Inject
    public DiskController() {
        
    }
    
    @Inject
    public void register(ServiceScheduler serviceScheduler, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        serviceScheduler.scheduleWithFixedDelay("DiskContrller.CacheCleaner", new CacheCleaner(), 10, 10, TimeUnit.MINUTES, backgroundExecutor);
    }
    
    /** Adds a DelayedWrite to the queue of writers. */
    public void addDelayedWrite(DelayedWrite dw) {
        synchronized(CACHE) {
            DELAYED.add(dw);
        }
    }
    
    /** Returns true if no delayed writes are pending. */
    public boolean canWriteNow() {
        synchronized(CACHE) {
            return DELAYED.isEmpty();
        }
    }
    
    /** Returns a chunk for writing.  Will return false if no chunks are available for writing. */
    public byte[] getWriteChunk() {
        return CACHE.getQuick();
    }
    
    /** Adds a job to be performed on the disk. */
    public void addDiskJob(final ChunkDiskJob job) {
        synchronized(SCHEDULE_LOCK) {
            chunksScheduled++;
            QUEUE.execute(new Runnable() {
                public void run() {
                    try {
                        job.runChunkJob(job.getChunk());
                    } finally {
                        synchronized(SCHEDULE_LOCK) {
                            chunksScheduled--;
                        }
                        
                        releaseChunk(job.getChunk(), true);
                        
                        job.finish();
                    }
                }
            });
        }
    }
    
    /** Adds a job to be performed on the disk that doesn't involve chunks. */
    public void addDiskJobWithoutChunk(Runnable job) {
        QUEUE.execute(job);
    }
    
    /** Gets a byte[] to the closest power of 2. */
    public byte[] getPowerOf2Chunk(int size) {
        return CHUNK_CACHE.get(size);
    }
    
    /**
     * A Runnable that clears the cache used for storing byte[]s used for
     * writing data read from network to disk, and schedules a ChunkCacheCleaner.
     */
    private class CacheCleaner implements Runnable {
        public void run() {
            LOG.info("clearing cache");
            CACHE.clear();
            QUEUE.execute(new ChunkCacheCleaner());
        }
    }
    
    /** A Runnable that clears the cache storing byte[]s used for verifying. */
    private class ChunkCacheCleaner implements Runnable {
        public void run() {
            CHUNK_CACHE.clear();
        }
    }

    private void releaseChunk(byte[] buf, boolean runDelayed) {
        CACHE.release(buf);
        if(runDelayed)
            runDelayedWrites();
    }

    private void runDelayedWrites() {
        synchronized(SCHEDULE_LOCK) {
            if(chunksScheduled > 0)
                return;
        }
        
        while(CACHE.isBufferAvailable()) {
            DelayedWrite dw;
            
            synchronized(CACHE) {
                if(DELAYED.isEmpty()) {
                    LOG.debug("Nothing delayed to run.");
                    return;
                }
                dw = DELAYED.get(0);
            }
    
            // write & notify outside of lock
            if(dw.write()) {
                // if we wrote succesfully, remove the item from the cache.
                synchronized(CACHE) {
                    DELAYED.remove(0);
                }
            } else {
                // otherwise, something went wrong, so reschedule another
                // delayed write later on.
                // NOTE: this should be impossible to happen, but it's happening,
                //       and its no huge deal, so we're preparing for it.
                QUEUE.execute(new Runnable() {
                    public void run() {
                        runDelayedWrites();
                    }
                });
            }
        }
    }

    /** Cleans the caches. */
    public void clearCaches() {
        Runnable runner = new CacheCleaner();
        runner.run();
    }

    /** Returns the number of bytes cached in the byte cache. */
    public int getSizeOfByteCache() {
        return CACHE.getCacheSize();
    }

    /** Returns the number of bytes cached in the verifying cache. */
    public int getSizeOfVerifyingCache() {
        return CHUNK_CACHE.getCacheSize();
    }

    public int getNumPendingItems() {
        return QUEUE.getQueue().size();
    }

}
