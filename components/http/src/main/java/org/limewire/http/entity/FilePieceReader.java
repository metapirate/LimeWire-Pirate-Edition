package org.limewire.http.entity;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.nio.ByteBufferCache;

/**
 * Reads chunks from a file into ByteBuffers.  
 */
public class FilePieceReader implements PieceReader {

    private static final Log LOG = LogFactory.getLog(FilePieceReader.class);

    /**
     * The number of concurrent threads used to read pieces of the file.
     */
    private static final int THREAD_COUNT = 2;

    /**
     * The number of buffers used to cache pieces. 
     */
    private static final int MAX_BUFFERS = THREAD_COUNT * 2;

    /**
     * The size of a single piece.
     */
    static int BUFFER_SIZE = 4096;

    /**
     * The list of buffers available for reading.
     * <p>
     * Note: Obtain <code>bufferPoolLock</code> when accessing.
     */
    private final List<ByteBuffer> bufferPool = new LinkedList<ByteBuffer>();

    /**
     * The list of cached pieces that have been read already sorted in ascending
     * order by file offset.
     * <p>
     * Note: Obtain <code>this</code> lock when accessing.
     */
    private final Queue<Piece> pieceQueue = new PriorityQueue<Piece>(
            MAX_BUFFERS);

    /**
     * Single queue that is used for all readers.
     */
    private final static ExecutorService QUEUE =
        ExecutorsHelper.newFixedSizeThreadPool(THREAD_COUNT, "DiskPieceReader");

    private final File file;

    private final PieceListener listener;

    private volatile FileChannel channel;

    private volatile RandomAccessFile raf;

    /**
     * Guards access to the buffer pool and offsets.
     * <p>
     * Order of locking: this -> bufferPoolLock 
     */
    private final Object bufferPoolLock = new Object();

    private final ByteBufferCache bufferCache;

    /**
     * Number of buffers currently in use by jobs.
     * <p>
     * Note: Obtain {@link #bufferPoolLock} before accessing.
     */
    private int bufferInUseCount;

    /**
     * The offset of the next piece that is going to be returned by
     * {@link #next()}. This field keeps track of the pieces read by the
     * consumer of this reader.
     */
    private volatile long readOffset;

    /**
     * The offset of the next piece that is going to be processed by a job. This
     * field keeps track of how far the file has been read from disk.
     * <p>
     * Note: Obtain {@link #bufferPoolLock} before accessing.
     */
    private long processingOffset;

    /**
     * The remaining number of bytes to read.
     * <p>
     * Note: Obtain {@link #bufferPoolLock} before accessing.
     */
    private long remaining;

    /**
     * If true, the reader has been shutdown.
     */
    private final AtomicBoolean shutdown = new AtomicBoolean();

    /**
     * Number of jobs currently processing pieces.
     */
    private final AtomicInteger jobCount = new AtomicInteger();

    public FilePieceReader(ByteBufferCache bufferCache, File file, long offset,
            long length, PieceListener listener) {
        if (bufferCache == null || file == null || listener == null) {
            throw new IllegalArgumentException();
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        
        this.bufferCache = bufferCache;
        this.file = file;
        this.readOffset = offset;
        this.processingOffset = offset;
        this.remaining = length;
        this.listener = listener;
    }

    /**
     * Invoked when data at <code>offset</code> has been read.
     */
    private void add(long offset, ByteBuffer buffer) {
        if (shutdown.get()) {
            release(buffer);
            return;
        }
        
        assert offset >= readOffset;

        Piece piece = new Piece(offset, buffer);
        synchronized (this) {
            pieceQueue.add(piece);
        }

        // only notify the listener if the next call to next() is guaranteed to
        // return a valid piece 
        if (offset == readOffset) {
            listener.readSuccessful();
        } else {
            if (LOG.isDebugEnabled()) {
                synchronized (this) {
                    LOG.debug("offset, readOffset: " + offset + ", " + readOffset);
                }
            }
        }
    }

    protected void failed(IOException exception) {
        shutdown();
        listener.readFailed(exception);
    }

    /**
     * Returns the file that is being read.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns true, if a {@link Piece} is available that can be retrieved
     * through {@link #next()}.
     */
    public synchronized boolean hasNext() throws EOFException {
        if (shutdown.get()) {
            throw new EOFException();
        }
        
        if (pieceQueue.isEmpty()) {
            return false;
        }
        return pieceQueue.peek().getOffset() == readOffset;
    }

    public boolean isShutdown() {
        return shutdown.get();
    }
    
    public synchronized Piece next() throws EOFException {
        if (shutdown.get()) {
            throw new EOFException();
        }
        
        synchronized (bufferPoolLock) {
            if (remaining == 0 && readOffset == processingOffset) {
                throw new EOFException();
            }
        }
        
        Piece piece = pieceQueue.peek();
        if (piece != null && piece.getOffset() == readOffset) {
            pieceQueue.remove();
            readOffset += piece.getBuffer().remaining();
            return piece;
        }
        return null;
    }

    private void release(ByteBuffer buffer) {       
        synchronized (bufferPoolLock) {
            if (shutdown.get()) {
                bufferCache.release(buffer);
                return;
            }
            
            bufferPool.add(buffer);
            bufferInUseCount--;

            spawnJobs();
        }
    }

    public void release(Piece piece) {
        release(piece.getBuffer());
    }

    /**
     * Releases all resources and shuts down the processing queue that reads the
     * file.
     * <p>
     * Once the reader is shutdown it is not possible to restart it. Does
     * nothing if shutdown has been invoked before;
     * 
     * @return false, if reader has already been shutdown
     */
    public boolean shutdown() {
        if (shutdown.getAndSet(true)) {
            return false;
        }
        
        synchronized (bufferPoolLock) {
            for (ByteBuffer buffer : bufferPool) {
                release(buffer);
            }
        }
        
        synchronized (this) {
            for (Piece piece : pieceQueue) {
                release(piece);
            }
        
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    LOG.warn("Error closing channel for file: " + file, e);
                }
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    LOG.warn("Error closing file: " + file, e);
                }
            }
        }
        
        return true;
    }

    public void shutdownAndWait(long timeout) throws InterruptedException, TimeoutException {
        shutdown();
        waitForShutdown(timeout);
    }
    
    protected void waitForShutdown(long timeout) throws InterruptedException, TimeoutException {
        long start = System.currentTimeMillis();
        while (jobCount.get() > 0) {
            long remainingTime = timeout - (System.currentTimeMillis() - start);
            if (remainingTime <= 0) {
                throw new TimeoutException();
            }
            Thread.sleep(50);
        }
    }
    
    /**
     * Starts the processing queue that reads the file. To free resources
     * {@link #shutdown()} must be invoked when reading has been completed.
     *
     * @throws IllegalStateException if reader is shutdown
     *
     */
    public void start() {
        if (shutdown.get()) {
            throw new IllegalStateException();
        }
        
        for (int i = 0; i < MAX_BUFFERS && (i == 0 || i * BUFFER_SIZE  + 1 <= remaining); i++) {
            bufferPool.add(bufferCache.getHeap(BUFFER_SIZE));
        }

        spawnJobs();
    }

    /**
     * Spawns additional reader jobs if buffers are available for reading.
     */
    private void spawnJobs() {       
        synchronized (bufferPoolLock) {
            while (!shutdown.get() && remaining > 0 && bufferInUseCount < MAX_BUFFERS) {
                int length = (int) Math.min(BUFFER_SIZE, remaining);
                ByteBuffer buffer = bufferPool.remove(0);
                bufferInUseCount++;
                Runnable job = new PieceReaderJob(buffer, processingOffset,
                        length);
                processingOffset += length;
                remaining -= length;
                jobCount.incrementAndGet();
                QUEUE.execute(job);
            }
        }
    }
    
    private void initChannel() throws IOException {
        if (channel != null) 
            return;
        synchronized(this) {
            if (channel != null)
                return;
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();
        }
    }

    /**
     * A simple job that reads a chunk of data form a file channel.
     */
    private class PieceReaderJob implements Runnable {

        private final ByteBuffer buffer;

        private final long offset;

        private final int length;

        public PieceReaderJob(ByteBuffer buffer, long offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }

        public void run() {
            try {
                if (shutdown.get()) {
                    release(buffer);
                    return;
                }
                

                buffer.clear();
                buffer.limit(length);

                IOException exception = null;
                try {
                    initChannel();
                    while (buffer.hasRemaining()) {
                        int read = channel.read(buffer, offset + buffer.position());
                        if (read == -1 || (read == 0 && raf.length() <= offset + buffer.position())) {
                            throw new EOFException("Attempt to read beyond end of file");
                        }
                    }
                } catch (IOException e) {
                    exception = e;
                }

                if (exception != null) {
                    try {
                        FilePieceReader.this.failed(exception);
                    } finally {
                        release(buffer);
                    }
                } else {
                    buffer.flip();
                    assert buffer.remaining() == length;

                    FilePieceReader.this.add(offset, buffer);
                }
            } finally {            
                jobCount.decrementAndGet();
            }
        }

    }

}
