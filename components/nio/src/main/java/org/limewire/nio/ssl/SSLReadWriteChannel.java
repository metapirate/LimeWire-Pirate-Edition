package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.channel.ChannelReader;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.util.BufferUtils;
import org.limewire.util.FileUtils;

/**
 * An SSL-capable layer that can transform incoming and outgoing
 * data according to the specified <code>SSLContext</code> and cipher suite.
 */
class SSLReadWriteChannel implements InterestReadableByteChannel, InterestWritableByteChannel,
                                 ChannelReader, ChannelWriter {
    
    private static final Log LOG = LogFactory.getLog(SSLReadWriteChannel.class);

    /** The context from which to retrieve a new SSLEngine. */
    private final SSLContext context;
    /** An executor to perform blocking tasks. */
    private final Executor sslBlockingExecutor;
    /** The engine managing this SSL session. */
    private SSLEngine engine;
    /** A temporary buffer which data is unwrapped to. */
    private ByteBuffer readIncoming;
    /** The buffer which the underlying readSink is read into. */
    private ByteBuffer readOutgoing;
    /** The buffer which we wrap writes to. */
    private ByteBuffer writeOutgoing;
    /** The underlying channel to read from. */
    private volatile InterestReadableByteChannel readSink;
    /** The underlying channel to write to. */
    private volatile InterestWritableByteChannel writeSink;
    /** The last WriteObserver that indicated write interested. */
    private volatile WriteObserver writeWanter;
    /** True if handshaking indicated we need to immediately perform a wrap. */
    private volatile boolean needsHandshakeWrap = false;
    /** True if handshaking indicated we need to immediately perform an unwrap. */
    private volatile boolean needsHandshakeUnwrap = false;
    /** True if a read finished and data was still buffered. */
    private volatile boolean readDataLeft = false;
    /** True only after a single read has been performed. */
    private final AtomicBoolean firstReadDone = new AtomicBoolean(false);
    
    /* Statistic gathering variables. */
    private volatile long readConsumed;
    private volatile long readProduced;
    private volatile long writeConsumed;
    private volatile long writeProduced;
    
    /**
     * Whether or not this has been shutdown.
     * Shutting down must be atomic with regard to initializing, so that
     * we can guarantee all allocated buffers are released
     * properly.
     * 
     * Shutdown is volatile so read/write/handleWrite can quickly
     * get it w/o locking.
     */
    private volatile boolean shutdown = false;
    private final Object initLock = new Object();
    
    /**
     * A lock used when changing to 'need task', to prevent
     * further writes/reads from changing interest.
     * Due to java bug: 6492872 
     *   (deadlock within SSLEngine if task/read/write called at same time,
     *    fixed in 1.6u2 & 1.7),
     * Internally we only do reads/writes on a single thread, so will
     * never encounter a read/write collision, but we always do
     * tasks on another thread, and it's possible that interest may
     * be turned on for reading|writing, which would cause a read|write,
     * and possibly an engine.wrap or engine.unwrap.
     * To workaround this, we prevent any interest from being turned on
     * once a task is performed.  When the task is finished, interest
     * will be turned on as necessary.
     */
    private final Object taskLock = new Object();
    private volatile boolean taskScheduled = false;
    
    /**
     * The last state of who interested us in reading must be kept,
     * so that after handshaking finishes, we can put reading into
     * the correct interest state.  otherwise, our options are:
     *  1) leave interest on, which could potentially loop forever
     *     if the connected socket closes.
     *  2) turn interest off, which could confuse any callers that
     *     had wanted to read data.
     * 
     * Note that we don't have to do this for writing because writing
     * can successfully turn itself off.
     */
    private boolean readInterest = false;
    private final Object readInterestLock = new Object();
    
    private final ByteBufferCache byteBufferCache;
    private final Executor networkExecutor;
    
    public SSLReadWriteChannel(SSLContext context, Executor sslBlockingExecutor,
            ByteBufferCache byteBufferCache, Executor networkExecutor) {
        this.sslBlockingExecutor = sslBlockingExecutor;
        this.context = context;
        this.byteBufferCache = byteBufferCache;
        this.networkExecutor = networkExecutor;
    }
    
    /**
     * Initializes this TLSLayer, using the given address and
     * enabling the given cipherSuites.
     * 
     * If clientMode is disabled, client authentication can be turned on/off.
     * 
     * @param addr
     * @param cipherSuites
     */
    void initialize(SocketAddress addr, String[] cipherSuites, boolean clientMode, boolean needClientAuth) {
        synchronized(initLock) {
            if(shutdown) {
                LOG.debug("Not initializing because already shutdown.");
                return;
            }
            
            if(addr != null) {
                if(!(addr instanceof InetSocketAddress))
                    throw new IllegalArgumentException("unsupported SocketAddress");
                InetSocketAddress iaddr = (InetSocketAddress)addr;
                String host = iaddr.getAddress().getHostAddress();
                int port = iaddr.getPort();
                engine = context.createSSLEngine(host, port);
            } else {
                engine = context.createSSLEngine();
            }
            if(cipherSuites != null) {
                engine.setEnabledCipherSuites(cipherSuites);
            }
            engine.setUseClientMode(clientMode);
            if(!clientMode) {
                engine.setWantClientAuth(needClientAuth);
                engine.setNeedClientAuth(needClientAuth);
            }
            SSLSession session = engine.getSession();
            readIncoming = byteBufferCache.getHeap(session.getPacketBufferSize());
            writeOutgoing = byteBufferCache.getHeap(session.getPacketBufferSize());
            if(LOG.isTraceEnabled())
                LOG.trace("Initialized engine: " + engine + ", session: " + session);
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        if(shutdown)
            throw new ClosedChannelException();
        
        // If a task is scheduled, don't read anything!
        if(taskScheduled)
            return 0;
        
        int transferred = 0;
        
        // If data was left in readOutgoing, pre-transfer it.
        if(readOutgoing != null && readOutgoing.position() > 0) {
            transferred += BufferUtils.transfer(readOutgoing, dst);
            if(readOutgoing.position() > 0) {
                LOG.debug("Transferred less than we have left!");
                return transferred;
            }
        }
        
        while(true) {
            // If we're not handshaking and there's no space to read into, exit early.
            // Must check separately for 'first read' and 'not handshaking', because
            // the engine isn't put into handshaking mode until a single read is done.
            if(firstReadDone.get() && !dst.hasRemaining() && engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
                LOG.debug("No room left to transfer data, exiting");
                return transferred;
            }

            int read = -1;
            while(readIncoming.hasRemaining() && (read = readSink.read(readIncoming)) > 0);
            // if we last read EOF & nothing was put in sourceBuffer, EOF
            if(read == -1 && readIncoming.position() == 0) {
                // TODO: Is this a better fix for EOF during handshaking?
                //if(!firstReadDone.get() || (engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING && engine.getHandshakeStatus() != HandshakeStatus.FINISHED))
                //    throw new ClosedChannelException();
                
                LOG.debug("Read EOF, no data to transfer.  Connection finished");
                return -1;
            }

            // If we couldn't read anything, there's nothing to unwrap.
            if(readIncoming.position() == 0) {
                LOG.debug("Unable to read anything, exiting read loop");
                return 0;
            }

            readIncoming.flip();

            // Try unwrapping directly into dst first.
            SSLEngineResult result = unwrap(engine, readIncoming, dst);
            readProduced += result.bytesProduced();
            readConsumed += result.bytesConsumed();
            transferred += result.bytesProduced();
            SSLEngineResult.Status status = result.getStatus();
            
            // If dst didn't have enough space, use an intermediate buffer.
            if(status == Status.BUFFER_OVERFLOW) {
                // Initialize readOutgoing only if not shutdown,
                // but grab the lock after we've checked to make sure
                // it's non-null, to avoid lock every read.
                // Lock is only necessary for 'shutdown'.
                if(readOutgoing == null) {
                    synchronized(initLock) {
                        if(shutdown)
                            throw new IOException("Shutdown while sizing");
                        
                        readOutgoing = byteBufferCache.getHeap(engine.getSession().getApplicationBufferSize());
                    }
                }
                result = unwrap(engine, readIncoming, readOutgoing);
                readProduced += result.bytesProduced();
                readConsumed += result.bytesConsumed();
                status = result.getStatus();
                if(status == Status.BUFFER_OVERFLOW) {
                    if(readIncoming.position() == 0 && readIncoming.capacity() == 16665 && engine.getSession().getPacketBufferSize() == 33049) {
                        // See: http://download.java.net/jdk7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLSession
                        // The SSL/TLS protocols specify 16KB as the buffer size, but some implementations
                        // do it wrong and make it 32KB.  The Sun impl will dynamically resize up, so we 
                        // need to handle that.
                        // (We grab the lock to make sure that the buffers aren't recycled as we do this.)
                        synchronized(initLock) {
                            if(shutdown)
                                throw new IOException("Shutdown while resizing.");
                            
                            // Transfer data from old readIncoming to newIncoming.
                            ByteBuffer newIncoming = byteBufferCache.getHeap(engine.getSession().getPacketBufferSize());
                            BufferUtils.transfer(readIncoming, newIncoming, false);
                            newIncoming.flip();
                            assert newIncoming.limit() == readIncoming.position();
                            assert newIncoming.position() == 0;
                            byteBufferCache.release(readIncoming);
                            readIncoming = newIncoming;
                            
                            // Replace outgoing with upgraded version.
                            assert readOutgoing.position() == 0;
                            byteBufferCache.release(readOutgoing);
                            readOutgoing = byteBufferCache.getHeap(engine.getSession().getApplicationBufferSize());
                            
                            // ... and try again!
                            result = unwrap(engine, readIncoming, readOutgoing);
                            readProduced += result.bytesProduced();
                            readConsumed += result.bytesConsumed();
                            status = result.getStatus();
                            if(status == Status.BUFFER_OVERFLOW)
                                throw new IllegalStateException("tried resizing, but still not enough room in fallback TLS buffer!  readOutgoing: " + readOutgoing + ", readIncoming: " + readIncoming + ", packet size: " + engine.getSession().getPacketBufferSize() + ", appl size: " + engine.getSession().getApplicationBufferSize());
                        }
                    } else {
                        throw new IllegalStateException("cannot resize, and not enough room in fallback TLS buffer!  readOutgoing: " + readOutgoing + ", readIncoming: " + readIncoming + ", packet size: " + engine.getSession().getPacketBufferSize() + ", appl size: " + engine.getSession().getApplicationBufferSize());
                    }
                }
                    
                transferred += BufferUtils.transfer(readOutgoing, dst);
            }
            
            firstReadDone.set(true);
            
            if(readIncoming.hasRemaining()) {
                readDataLeft = true;
                readIncoming.compact();
            } else {
                readDataLeft = false;
                readIncoming.clear();
            }
            
            if(LOG.isDebugEnabled())
                LOG.debug("Read unwrap result: " + result + ", transferred: " + transferred);
            
            
            // If we were unable to interpret this packet because not enough was
            // read, then we must exit and wait for more to be read later.
            if(status == Status.BUFFER_UNDERFLOW) {
                if(transferred == 0 && read == -1) {
                    LOG.debug("Read EOF & underflow when unwrapping.  Connection finished");
                    return -1;
                } else {
                    return transferred;
                }
            }
            
            // If the engine is closed and nothing was transferred,
            // return -1 to show the stream ended.  Otherwise return
            // however much we were able to already transfer.
            if(status == Status.CLOSED) {
                if(transferred == 0)
                    return -1;
                else
                    return transferred;
            }
            
            // We may be handshaking, which requires processing of data...
            if(!processHandshakeResult(true, false, result.getHandshakeStatus()))
                return transferred;
        }
    }
    

	/** See {@link SSLEngine#wrap(ByteBuffer, ByteBuffer) */
    private SSLEngineResult wrap(SSLEngine engine, ByteBuffer src, ByteBuffer dst) throws SSLException, IOException {
        assert src != null;
        assert dst != null;
        try {
            return engine.wrap(src, dst);
        } catch(RuntimeException re) {
            throw new IOException(re);
        } catch(Error e) {
            throw new IOException(e);
        }
    }
    
    /** See {@link SSLEngine#unwrap(ByteBuffer, ByteBuffer) */
    private SSLEngineResult unwrap(SSLEngine engine, ByteBuffer src, ByteBuffer dst) throws SSLException, IOException {
        assert dst != null;
        assert src != null;
        try {
            return engine.unwrap(src, dst);
        } catch(RuntimeException re) {
            throw new IOException(re);
        } catch(Error e) {
            throw new IOException(e);
        }
    }

    /**
     * Processes a single handshake result.
     * If a delegated task is needed, returns false & schedules the task(s).
     * If writing is needed, returns false only if currently reading.
     * If reading is needed, returns false only if currently writing.
     * Otherwise, returns true.
     */
    private boolean processHandshakeResult(boolean reading, boolean writing, HandshakeStatus hs) {
        if(LOG.isTraceEnabled())
            LOG.trace("Processing result from: " + engine + ", result: " + hs);
        
        needsHandshakeWrap = false;
        needsHandshakeUnwrap = false;
        switch(hs) {
        case NEED_TASK:
            needTask();
            return false;
        case NEED_WRAP:
            needsHandshakeWrap = true;
            // IMPORTANT:  read interest must be turned off before write
            //             interest is turned on.  This is necessary because
            //             if write interest is turned on from the TASK thread,
            //             it can get immediate notification from NIODispatcher,
            //             which can continue processing more results, and will
            //             set read interest on.  Then, when the context switches
            //             back to this thread, it would turn read interest off.
            //             So: TURN READ INTEREST OFF FIRST!
            readSink.interestRead(false);
            writeSink.interestWrite(this, true);
            return writing;
        case NEED_UNWRAP:
            // IMPORTANT:  write interest must be turned off before write
            //             interest is turned on.  This is necessary because
            //             if read interest is turned on from the TASK thread,
            //             it can get immediate notification from NIODispatcher,
            //             which can continue processing more results, and will
            //             set read interest on.  Then, when the context switches
            //             back to this thread, it would turn write interest off.
            //             So: TURN WRITE INTEREST OFF FIRST!
            
            writeSink.interestWrite(null, false);
            synchronized(readInterestLock) {
                needsHandshakeUnwrap = true;
                readSink.interestRead(true);
            }
            // If we had previously buffered read data, force a read.
            if(readDataLeft && !reading)
                networkExecutor.execute(new Runnable() {
                    public void run() {
                        try {
                            read(BufferUtils.getEmptyBuffer());
                        } catch(IOException iox) {
                            FileUtils.close(SSLReadWriteChannel.this);
                        }
                    }
                });
            return reading;
        case FINISHED:
            synchronized(readInterestLock) {
                // set interest to what our observer wanted.
                readSink.interestRead(readInterest);
            }
            writeSink.interestWrite(this, true);
        case NOT_HANDSHAKING:
        default: 
            // no change.
            return true;
        }
    }
    
    /** The engine needs to run some tasks before proceeding... */
    private void needTask() {
        synchronized(taskLock) {
            taskScheduled = true;
            readSink.interestRead(false);
            writeSink.interestWrite(null, false);
        }
        // Run as many tasks as possible, and then add another
        // that will process the next state.
        while(true) {
            final Runnable runner = engine.getDelegatedTask();
            if(runner == null) {
                sslBlockingExecutor.execute(new Runnable() {
                    public void run() {
                        synchronized(taskLock) {
                            taskScheduled = false;
                        }
                        HandshakeStatus status = engine.getHandshakeStatus();
                        if(LOG.isDebugEnabled())
                            LOG.debug("Task(s) finished, status: " + status);
                        processHandshakeResult(false, false, status);
                    }
                });
                break;
            } else {
                sslBlockingExecutor.execute(runner);
            }
        }
    }

    public int write(ByteBuffer src) throws IOException {
        if(shutdown)
            throw new ClosedChannelException();
        
        // If a task is scheduled, don't write anything!
        if(taskScheduled)
            return 0;
        
        int consumed = 0;
        // do...while because we want to force one write even with empty buffers
        do {
            boolean wasEmpty = writeOutgoing.position() == 0;
            SSLEngineResult result = wrap(engine, src, writeOutgoing);
            writeProduced += result.bytesProduced();
            writeConsumed += result.bytesConsumed();
            if(LOG.isDebugEnabled())
                LOG.debug("Wrap result: " + result);
            consumed += result.bytesConsumed();
            SSLEngineResult.Status status = result.getStatus();
            if(status == Status.CLOSED && !isOpen())
                throw new ClosedChannelException();
            if(!processHandshakeResult(false, true, result.getHandshakeStatus()))
                break;
            if(status == Status.BUFFER_OVERFLOW) {
                if(wasEmpty)
                    throw new IllegalStateException("outgoing TLS buffer not large enough!");
                else
                    break;
            }
        } while(src.hasRemaining());
        
        return consumed;
    }

    public boolean handleWrite() throws IOException {
        if(shutdown)
            throw new ClosedChannelException();
        
        InterestWritableByteChannel source = writeSink;
        if(source == null)
            throw new IllegalStateException("writing with no source.");
            
        while(true) {
            if(writeOutgoing.position() > 0) {
                // Step 1: See if there is any pending data to be written.
                writeOutgoing.flip();
                writeSink.write(writeOutgoing);
                if(writeOutgoing.hasRemaining()) {
                    writeOutgoing.compact();
                    return true; // there is still data that is pending a write.
                }            
                writeOutgoing.clear();
            }

            // Step 2: If we need to do a handshake wrap, do that.
            if(needsHandshakeWrap) {
                LOG.debug("Forcing a handshake wrap");
                write(BufferUtils.getEmptyBuffer());
                if(writeOutgoing.position() > 0)
                    continue;
            }
                            
            // Step 3: Tell any interested parties to write data.
            WriteObserver interested = writeWanter;
            if(interested != null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Telling interested parties to write.  (a " + interested + ")");
                interested.handleWrite();
            }
            
            // If no data after that, we've written everything we want -- exit.
            if (writeOutgoing.position() == 0) {
                // We have nothing left to write, however, it is possible
                // that between the above check for interested.handleWrite & here,
                // we got pre-empted and another thread turned on interest.
                synchronized (this) {
                    if (writeWanter == null) // no observer? good, we can turn interest off
                        source.interestWrite(this, false);
                    // else, we've got nothing to write, but our observer might.
                }
                return false;
            }
        }
    }

    /**
     * Releases any resources that were acquired by the channel.
     * If the underlying channels are still open, this method only propogates
     * the shutdown call, instead of shutting down this channel, as it can
     * still be used by other channels.
     */
    public void shutdown() {
        synchronized(initLock) {
            if(shutdown)
                return;
            
            if(!isOpen()) {
                LOG.debug("Shutting down SSL channel");
                shutdown = true;
            }
        }
        
        if(shutdown) {
            networkExecutor.execute(new Runnable() {
                public void run() {
                    if(readIncoming != null)
                        byteBufferCache.release(readIncoming);
                    if(readOutgoing != null)
                        byteBufferCache.release(readOutgoing);
                    if(writeOutgoing != null)
                        byteBufferCache.release(writeOutgoing);
                }
            });
        }
        
        Shutdownable observer = writeWanter;
        if(observer != null)
            observer.shutdown();
    }

    public InterestReadableByteChannel getReadChannel() {
        return readSink;
    }

    public void setReadChannel(InterestReadableByteChannel newChannel) {
        this.readSink = newChannel;
        
    }

    public InterestWritableByteChannel getWriteChannel() {
        return writeSink;
    }

    public void setWriteChannel(InterestWritableByteChannel newChannel) {
        this.writeSink = newChannel;
    }

    public void close() throws IOException {
        readSink.close();
        writeSink.close();
    }

    public boolean isOpen() {
        return readSink != null && readSink.isOpen() && writeSink != null && writeSink.isOpen();
    }

    public void handleIOException(IOException iox) {
        shutdown();
    }
    
    public void interestRead(boolean status) {
        synchronized(taskLock) {
            synchronized(readInterestLock) {
                readInterest = status;
                boolean interest = !taskScheduled && (needsHandshakeUnwrap || status);
                readSink.interestRead(interest);
            }
        }
    }

    public synchronized void interestWrite(WriteObserver observer, boolean status) {
        this.writeWanter = status ? observer : null;
        InterestWritableByteChannel source = writeSink;
        if(source != null) {
            synchronized(taskLock) {
                source.interestWrite(this, !taskScheduled);
            }
        }
    }
    
    /** Returns the total number of bytes that this has produced from unwrapping reads. */
    long getReadBytesProduced() {
        return readProduced;
    }
    
    /** Returns the total number of bytes that this has consumed while unwrapping reads. */
    long getReadBytesConsumed() {
        return readConsumed;
    }
    
    /** Returns the total number of bytes that this has produced from wrapping writes. */
    long getWrittenBytesProduced() {
        return writeProduced;
    }
    
    /** Returns the total number of bytes that this has consumed while wrapping writes. */
    long getWrittenBytesConsumed() {
        return writeConsumed;
    }
    
    /** Returns the SSLSession this channel uses. */
    SSLSession getSession() {
        return engine != null ? engine.getSession() : null;
    }
    
    /** Returns true if we're currently handshaking. */
    boolean isHandshaking() {
        return !firstReadDone.get() || engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING;
        
    }

    public boolean hasBufferedOutput() {
        InterestWritableByteChannel channel = this.writeSink;
        return writeOutgoing.position() > 0 || (channel != null && channel.hasBufferedOutput());
    }
}
