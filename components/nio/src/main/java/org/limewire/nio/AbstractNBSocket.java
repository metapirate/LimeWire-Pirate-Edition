package org.limewire.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelReader;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.channel.ThrottleReader;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.ReadObserver;
import org.limewire.nio.observer.ReadWriteObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.nio.timeout.ReadTimeout;
import org.limewire.nio.timeout.SoTimeout;

/**
 * Implements all common functionality that a non-blocking socket must contain. 
 * Specifically, <code>AbstractNBSocket</code> handles 
 * the multiplexing aspect of handing off reading, writing and connecting to 
 * other Observers (<code>org.limewire.nio.observer</code>). 
 * <p>
 * Additionally, <code>AbstractNBSocket</code> traverses the chain of readers 
 * and writers to read leftover data and ensure remaining data is written. 
 * <p>
 * <code>AbstractNBSocket</code> also exposes a common blocking input and output
 * stream.
 */
public abstract class AbstractNBSocket extends NBSocket implements ConnectObserver, ReadWriteObserver,
                                                                  NIOMultiplexor, ReadTimeout, SoTimeout{
    
    private static final Log LOG = LogFactory.getLog(AbstractNBSocket.class);

    /** Lock for shutting down. */
    private final Object LOCK = new Object();

    /** The reader. */
    private volatile ChannelReadObserver reader;
    
    /** The throttle read channel. */
    private volatile ThrottleReader throttleReader;

    /** The writer. */
    private volatile WriteObserver writer;
    
    /** The NIOOutputStream object, if we're using blocking writing. */
    private volatile NIOOutputStream nioOutputStream;

    /** The connecter. */
    private volatile ConnectObserver connecter;
    
    /** An observer for being shutdown. */
    private volatile Shutdownable shutdownObserver;

    /** Whether or not we've shutdown the socket. */
    private boolean shutdown = false;
    
    /**
     * Retrieves the channel which should be used as the base channel
     * for all reading operations.
     */
    protected abstract InterestReadableByteChannel getBaseReadChannel();
    
    /**
     * Retrieves the channel which should be used as the base channel
     * for all writing operations.
     * <p>
     * If the base write channel is chained (that is, if there are multiple
     * writing layers that will always be used) then this must return
     * the top-most layer.  That layer will be installed beneath the
     * bottom layer that is set on the Socket.  All layers except the last
     * must implement ChannelWriter, so they can be iterated over in order
     * to set the last writer.
     */
    protected abstract InterestWritableByteChannel getBaseWriteChannel();
    
    /**
     * Performs any operations required for shutting down this socket.
     * <code>shutdownImpl</code> method will only be called once per Socket.
     */
    protected abstract void shutdownImpl();
    
    /**
     * Sets the initial reader value.
     */
    public final void setInitialReader() {
        reader = new NIOInputStream(this, this, getBaseReadChannel());
    }
    
    /**
     * Sets the initial writer value.
     */
    public final void setInitialWriter() {
        InterestWritableByteChannel base = getBaseWriteChannel();
        writer = getBottomFromChain(base);
        nioOutputStream = new NIOOutputStream(this, base);
    }
    
    private InterestWritableByteChannel getBottomFromChain(InterestWritableByteChannel top) {
        if(top instanceof ChannelWriter) {
            ChannelWriter lastChannel = (ChannelWriter)top;
            while(lastChannel.getWriteChannel() instanceof ChannelWriter)
                lastChannel = (ChannelWriter)lastChannel.getWriteChannel();
            return (InterestWritableByteChannel)lastChannel;
        } else {
            return top;
        }
    }
    
    /**
     * Sets the <code>Shutdown</code> observer.
     * This observer is useful for when the Socket is created,
     * but connect has not been called yet.  This observer will be
     * notified when the socket is shutdown.
     */
    @Override
    public final void setShutdownObserver(Shutdownable observer) {
        shutdownObserver = observer;
    }

    /** Sets the new throttle for reading. */
    public final void setReadThrottleChannel(final ThrottleReader newThrottle) {
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                synchronized(LOCK) {
                    if(shutdown) {
                        return;
                    }
                }
                
                assert throttleReader == null;
                assert newThrottle != null;
                throttleReader = newThrottle;                
                throttleReader.setAttachment(AbstractNBSocket.this);                
                installThrottle(throttleReader, reader);
            }
        });
    }

    /**
     * Inserts the ThrottleReader into the reader chain. This will find the
     * lowest source of the chain & set the ThrottleReader to the read channel
     * of that source, and then set the read channel of the throttle to be the
     * {@link #getBaseReadChannel() base read channel}.
     */
    protected void installThrottle(ThrottleReader throttle, ChannelReader reader) {
        ChannelReader lastChannel = reader;
        // go down the chain of ChannelReaders and find the last one to set our source
        while(lastChannel.getReadChannel() instanceof ChannelReader) {
            lastChannel = (ChannelReader)lastChannel.getReadChannel();
        }
        
        if(throttle != lastChannel) {
            lastChannel.setReadChannel(throttle);
            throttle.setReadChannel(getBaseReadChannel());
        }
    }
    
    /**
     * Sets the new <code>ReadObserver</code>.
     * <p>
     * The deepest <code>ChannelReader</code> in the chain first has its source
     * set to the prior reader (assuming it implemented <code>ReadableByteChannel</code>)
     * and a read is notified, in order to read any buffered data.
     * The source is then set to the Socket's channel and interest
     * in reading is turned on.
     */
    public final void setReadObserver(final ChannelReadObserver newReader) {
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                ReadObserver oldReader = reader;
                try {
                    synchronized(LOCK) {
                        if(shutdown) {
                            newReader.shutdown();
                            return;
                        }
                        reader = newReader;
                    }
                    
                    // At this point, if the socket gets shutdown, we know the
                    // reader is going to be notified of the shutdown.
                    
                    ChannelReader lastChannel = newReader;
                    // go down the chain of ChannelReaders and find the last one to set our source
                    while(lastChannel.getReadChannel() instanceof ChannelReader)
                        lastChannel = (ChannelReader)lastChannel.getReadChannel();
                    
                    if(lastChannel instanceof RequiresSelectionKeyAttachment)
                    	((RequiresSelectionKeyAttachment)lastChannel).setAttachment(AbstractNBSocket.this);
                    
                    if(oldReader instanceof InterestReadableByteChannel && oldReader != newReader) {
                        lastChannel.setReadChannel((InterestReadableByteChannel)oldReader);
                        reader.handleRead(); // read up any buffered data from the old reader chain.
                        oldReader.shutdown(); // shutdown the now unused reader.
                    }
                    
                    InterestReadableByteChannel source = getBaseReadChannel();
                    lastChannel.setReadChannel(source);
                    
                    // Insert the throttle, if one has been set.
                    if(throttleReader != null) {
                        installThrottle(throttleReader, reader);
                    }

                    source.interestRead(true);
                    
                    // If the socket is still connected, read up any buffered data from the current chain.
                    // This is only done if we know the dispatcher is not going to immediately call
                    // handleRead on the socket.  If this were not done, buffered data within the chain
                    // would not be read until future incoming data triggered another handleRead.
                    if(isConnected() && !NIODispatcher.instance().isReadReadyThisIteration(getChannel()))
                        reader.handleRead();
                } catch(IOException iox) {
                    shutdown();
                    oldReader.shutdown(); // in case we lost it.
                }
            }
        });
    }
    
    /**
     * Sets the new <code>WriteObserver</code>.
     *<p>
     * If a <code>ThrottleWriter</code> is one of the <code>ChannelWriters</code>, 
     * the attachment of the <code>ThrottleWriter</code> is set to be this.
     * <p>
     * The deepest <code>ChannelWriter<code> in the chain has its source set to be
     * a new <code>InterestWriteChannel</code>, which will be used as the hub to receive
     * and forward interest events from/to the channel.
     * <p>
     * If this is called while the existing <code>WriteObserver</code> still has data left to
     * write, then an <code>IllegalStateException</code> is thrown.
     */
    public final void setWriteObserver(final ChannelWriter newWriter) {
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                try {
                    if(writer.handleWrite())
                        throw new IllegalStateException("data still in old writer!");
                    writer.shutdown();
                    // Guarantee the NIOOutputStream is closed, if it existed.
                    if(nioOutputStream  != null)
                        nioOutputStream.shutdown();

                    ChannelWriter lastChannel = newWriter;
                    while(lastChannel.getWriteChannel() instanceof ChannelWriter) {
                        lastChannel = (ChannelWriter)lastChannel.getWriteChannel();
                        if(lastChannel instanceof RequiresSelectionKeyAttachment)
                            ((RequiresSelectionKeyAttachment)lastChannel).setAttachment(AbstractNBSocket.this);
                    }

                    InterestWritableByteChannel source = getBaseWriteChannel();
                    
                    synchronized(LOCK) {
                        lastChannel.setWriteChannel(source);
                        if(shutdown) {
                            source.shutdown();
                            return;
                        }
                        nioOutputStream = null;
                        writer = getBottomFromChain(source);
                    }
                } catch(IOException iox) {
                    shutdown();
                    newWriter.shutdown(); // in case we hadn't set it yet.
                }
            }
       });
   }
    
    /**
     * Notification that a connect can occur.
     * <p>
     * This passes it off on to the delegating connecter and then forgets the
     * connecter for the duration of the connection.
     */
    public final void handleConnect(Socket s) throws IOException {
        // Clear out connector prior to calling handleConnect.
        // This is so that if handleConnect throws an IOX, the
        // observer won't be confused by having both handleConnect &
        // shutdown called.  It'll be one or the other.
        ConnectObserver observer = connecter;
        connecter = null;
        observer.handleConnect(this);
    }
    
    /**
     * Notification that a read can occur.
     * <p>
     * This passes it off to the delegating reader.
     */
    public final void handleRead() throws IOException {
        reader.handleRead();
    }
    
    /**
     * Notification that a write can occur.
     * <p>
     * This passes it off to the delegating writer.
     */
    public final boolean handleWrite() throws IOException {
        return writer.handleWrite();
    }
    
    /** Closes the socket & all streams, waking up any waiting locks.  */
    @Override
    public final void close() {
        shutdown();
    }
    
    /** Connects to <code>addr</code> with no timeout. */
    @Override
    public final void connect(SocketAddress addr) throws IOException {
        connect(addr, 0);
    }
    
    /** Connects to <code>addr</code> with the given timeout (in milliseconds). */
    @Override
    public final void connect(SocketAddress addr, int timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must not be < 0");
        }
        
        final CountDownLatch connectLatch = new CountDownLatch(1);
        ConnectObserver connecter = new ConnectObserver() {
            public void handleConnect(Socket s) { connectLatch.countDown(); }
            public void shutdown() { connectLatch.countDown(); }            
            // unused
            public void handleIOException(IOException e) { }
        };

        if(!connect(addr, timeout, connecter)) {
            long then = System.currentTimeMillis();
            try {
                if (timeout == 0) {
                    connectLatch.await();
                } else {
                    // wait a little extra to allow other threads to notify
                    connectLatch.await(timeout + 1000, TimeUnit.MILLISECONDS);
                }
            } catch(InterruptedException ie) {
                shutdown();
                throw new InterruptedIOException(ie);
            }

            if(!isConnected()) {
                shutdown();
                long now = System.currentTimeMillis();
                if(timeout != 0 && now - then >= timeout)
                    throw new SocketTimeoutException("operation timed out (" + timeout + ")");
                else
                    throw new ConnectException("Unable to connect!");
            }
        }
    }
    
    /**
     * Connects to the specified address within the given timeout (in milliseconds).
     * The given <code>ConnectObserver</code> will be notified of success or failure.
     * In the event of success, <code>observer.handleConnect</code> is called. In a failure,
     * <code>observer.shutdown</code> is called. <code>observer.handleIOException</code> 
     * is never called.
     * <p>
     * Returns true if this was able to connect immediately. The observer is still
     * notified about the success even it it was immediate.
     */
    @Override
    public boolean connect(SocketAddress addr, int timeout, final ConnectObserver observer) {
        synchronized(LOCK) {
            if(shutdown) {
                observer.shutdown();
                return false;
            }
            
            // Set the connectObserver within the lock so that the connecter
            // will not be set away from null after shutdown is called.
            this.connecter = observer;
        }
        
        // At this point, we know that if shutdown is called, the observer
        // will be notified of the shutdown.
        
        try {
            InetSocketAddress iaddr = (InetSocketAddress)addr;
            if (iaddr.isUnresolved())
                throw new IOException("unresolved: " + addr);
            
            if(getChannel().connect(addr)) {
                // Make sure connecting callbacks are always on the NIO thread.
                NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
                    public void run() {
                        // ensure it's registered in the selector, so it can be notified
                        // for reading|writing, and polled for readiness
                        NIODispatcher.instance().register(getChannel(), AbstractNBSocket.this);
                        try {
                            observer.handleConnect(AbstractNBSocket.this);
                        } catch(IOException iox) {
                            NIODispatcher.instance().executeLaterAlways(new Runnable() {
                                public void run() {
                                    shutdown();
                                }
                            });
                        }
                    }
                });
                return true;
            } else {
                NIODispatcher.instance().registerConnect(getChannel(), this, timeout);
                return false;
            }
        } catch(IOException failed) {
            NIODispatcher.instance().executeLaterAlways(new Runnable() {
                public void run() {
                    shutdown();
                }
            });
            return false;
        }
    }


    
    /**
     * Returns the <code>InputStream</code> from the <code>NIOInputStream</code>.
     * <p>
     * Internally, this is a blocking Pipe from the non-blocking <code>SocketChannel</code>.
     */
    @Override
    public final InputStream getInputStream() throws IOException {
        // Unlocked check real quickly.
        if(isClosed() || isShutdown())
            throw new IOException("Socket closed.");
        
        ReadObserver localReader;
        synchronized(LOCK) {
            if(isShutdown())
                throw new IOException("Socket closed.");
            localReader = reader;
        }
        
        if(localReader instanceof NIOInputStream) {
            NIOInputStream nis = (NIOInputStream)localReader;
            // Ensure the stream is initialized before we interest it.
            InputStream stream = nis.getInputStream();
            nis.interestRead(true);
            return stream;
        } else {
            Callable<InputStream> callable = new Callable<InputStream>() {
                public InputStream call() throws IOException {
                    NIOInputStream stream = new NIOInputStream(AbstractNBSocket.this, AbstractNBSocket.this, null).init();
                    setReadObserver(stream);
                    return stream.getInputStream();
                }
            };
            
            Future<InputStream> future = NIODispatcher.instance().getScheduledExecutorService().submit(callable);
            try {
                return future.get();
            } catch(ExecutionException ee) {
                throw (IOException)new IOException().initCause(ee.getCause());
            } catch (InterruptedException ie) {
                throw (IOException)new IOException().initCause(ie.getCause());
            }
        }
    }
    
    /**
     * Returns the <code>OutputStream</code> from the <code>NIOOutputStream</code>.
     * <p>
     * Internally, this is a blocking Pipe from the non-blocking SocketChannel.
     */
    @Override
    public final OutputStream getOutputStream() throws IOException {
        // Unlocked check real quickly.
        if(isClosed() || isShutdown())
            throw new IOException("Socket closed.");
        
        // Grab a handle to the stream, to ensure it can't become null.
        NIOOutputStream output = nioOutputStream;
        
        if(output != null) {
            return output.getOutputStream();
        } else {
            Callable<OutputStream> callable = new Callable<OutputStream>() {
                @Override
                public OutputStream call() throws Exception {
                    InterestWritableByteChannel source = getBaseWriteChannel();
                    InterestWritableByteChannel bottom = getBottomFromChain(source);
                    if (bottom.hasBufferedOutput()) {
                        throw new IllegalStateException("still buffered output");
                    } else {
                        synchronized (LOCK) {
                            if (shutdown) {
                                throw new IOException("shut down");
                            }
                            nioOutputStream = new NIOOutputStream(AbstractNBSocket.this, source);
                            writer = getBottomFromChain(source);
                            return nioOutputStream.getOutputStream();
                        }
                    }
                }
            };
            Future<OutputStream> future = NIODispatcher.instance().getScheduledExecutorService().submit(callable);
            try {
                return future.get();
            } catch(ExecutionException ee) {
                throw new IOException(ee.getCause());
            } catch (InterruptedException ie) {
                throw new IOException(ie.getCause());
            }
        }
    }
    
    /** Gets the read timeout for this socket. */
    public long getReadTimeout() {
        if(reader instanceof NIOInputStream) {
            return 0; // NIOInputStream handles its own timeouts.
        } else {
            try {
                return getSoTimeout();
            } catch(SocketException se) {
                return 0;
            }
        }
    }    
    
    /**
     * Notification that an <code>IOException</code> occurred while processing a
     * read, connect, or write.
     */
    public final void handleIOException(IOException iox) {
        if (LOG.isDebugEnabled())
            LOG.debug(this, iox);
        shutdown();
    }
    
    /**
     * Shuts down this socket & all its streams.
     */
    public final void shutdown() {
        synchronized(LOCK) {
            if(shutdown)
                return;
            shutdown = true;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Shutting down socket & streams for: " + this);
 
        // NOTE: We assume >= Java 1.5.0_10.
        //       Otherwise we'd need to workaround bugid: 4744057.
        shutdownSocketAndChannels();
        shutdownObservers();
                
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                if(nioOutputStream != null)
                    nioOutputStream.shutdown();
                nioOutputStream = null;
                reader = new NoOpReader();
                writer = new NoOpWriter();
                throttleReader = null;
                connecter = null;
                shutdownObserver = null;
            }
        });
    }
    
    /** Shuts down all observers. */
    protected void shutdownObservers() {
        reader.shutdown();
        writer.shutdown();
        if(connecter != null)
            connecter.shutdown();
        if(shutdownObserver != null)
            shutdownObserver.shutdown();
    }
    
    /** Shuts down the socket and channels. */
    private void shutdownSocketAndChannels() {
        shutdownImpl();
        try {
            getChannel().close();
        } catch(IOException ignored) {}
    }
    
    private boolean isShutdown() {
        synchronized(LOCK) {
            return shutdown;
        }
    }
    
}
