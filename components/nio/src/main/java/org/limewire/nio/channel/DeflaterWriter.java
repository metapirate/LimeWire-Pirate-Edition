package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.zip.Deflater;

import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;



/**
 * Deflates data written to this channel and writes the deflated
 * data to another channel sink.
 */
public class DeflaterWriter implements ChannelWriter, InterestWritableByteChannel {
    
    //private static final Log LOG = LogFactory.getLog(DeflaterWriter.class);
    
    /** The channel to write to & interest on. */    
    private volatile InterestWritableByteChannel channel;
    /** The next observer. */
    private volatile WriteObserver observer;
    /** The buffer used for deflating into. */
    private ByteBuffer outgoing;
    /** The buffer used for writing data into. */
    private ByteBuffer incoming;
    /** The deflater to use */
    private Deflater deflater;
    /** The sync level we're on.  0: not sync, 1: NO_COMPRESSION, 2: DEFAULT */
    private int sync = 0;
    /** An empty byte array to reuse. */
    private static final byte[] EMPTY = new byte[0];
        
    /**
     * Constructs a new <code>DeflaterWriter</code> with the given deflater.
     * <p>
     * <b>NOTE:</b> You must call <code>setWriteChannel</code> prior to 
     * <code>handleWrite</code>.
     */
    public DeflaterWriter(Deflater deflater) {
        this(deflater, null);
    }
    
    /**
     * Constructs a new <code>DeflaterWriter</code> with the given deflater 
     * and channel.
     */
    public DeflaterWriter(Deflater deflater, InterestWritableByteChannel channel) {
        this.deflater = deflater;
        this.incoming = ByteBuffer.allocate(4 * 1024);
        this.outgoing = ByteBuffer.allocate(512);
        outgoing.flip();
        this.channel = channel;
    }
    
    /** {@inheritDoc} */
    public InterestWritableByteChannel getWriteChannel() {
        return channel;
    }
    
    /** {@inheritDoc} */
    public void setWriteChannel(InterestWritableByteChannel channel) {
        this.channel = channel;
        channel.interestWrite(this, true);
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void interestWrite(WriteObserver observer, boolean status) {
        this.observer = status ? observer : null;
        
        // just always set interest on.  it's easiest & it'll be turned off
        // immediately once we're notified if we don't wanna do anything.
        // note that if we did want to do it correctly, we'd have to check
        // incoming.hasRemaining() || outgoing.hasRemaining(), but since
        // interest can be called in any thread, we'd have to introduce
        // locking around incoming & outgoing, which just isn't worth it.
        InterestWritableByteChannel source = channel;
        if(source != null)
            source.interestWrite(this, true); 
    }
    
    /**
     * Writes data to our internal buffer, if there's room.
     */
    public int write(ByteBuffer buffer) throws IOException {
        int wrote = 0;
        
        if(incoming.hasRemaining()) {
            int remaining = incoming.remaining();
            int adding = buffer.remaining();
            if(remaining >= adding) {
                incoming.put(buffer);
                wrote = adding;
            } else {
                int oldLimit = buffer.limit();
                int position = buffer.position();
                buffer.limit(position + remaining);
                incoming.put(buffer);
                buffer.limit(oldLimit);
                wrote = remaining;
            }
        }
        
        return wrote;
    }
    
    /** Closes the underlying channel. */
    public void close() throws IOException {
        Channel source = channel;
        if(source != null)
            source.close();
    }
    
    /** Determines if the underlying channel is open. */
    public boolean isOpen() {
        Channel source = channel;
        return source != null ? source.isOpen() : false;
    }
    
    /**
     * Writes as much data as possible to the underlying source.
     * This tries to write any previously unwritten data, then tries
     * to deflate any new data, then tries to get more data by telling
     * its interested-observer to write to it.  This continues until
     * there is no more data to be written or the sink is full.
     */
    public boolean handleWrite() throws IOException {
        InterestWritableByteChannel source = channel;
        if(source == null)
            throw new IllegalStateException("writing with no source.");
            
        while(true) {
            // Step 1: See if there is any pending deflated data to be written.
            channel.write(outgoing);
            if(outgoing.hasRemaining())
                return true; // there is still deflated data that is pending a write.

            while(true) {
                // Step 2: Try and deflate the existing data.
                int deflated;
                try {
                    deflated = deflater.deflate(outgoing.array());
                } catch(NullPointerException npe) {
                    // stupid deflater not supporting asynchronous ends..
                    throw (IOException) new IOException().initCause(npe);
                }
                if(deflated > 0) {
                    outgoing.position(0).limit(deflated);
                    break; // we managed to deflate some data, try to write it...
                }
                    
                // Step 3: Normal deflate didn't work, try to simulate a Z_SYNC_FLUSH
                // Note that this requires we tried deflating until deflate returned 0
                // above.  Otherwise, this setInput call would erase prior input.
                // We must use different levels of syncing because we have to make sure
                // that we write everything out of deflate after each level is set.
                // Otherwise compression doesn't work.
                try {
                    if(sync == 0) {
                        deflater.setInput(EMPTY);
                        deflater.setLevel(Deflater.NO_COMPRESSION);
                        sync = 1;
                        continue;
                    } else if(sync == 1) {
                        deflater.setLevel(Deflater.DEFAULT_COMPRESSION);
                        sync = 2;
                        continue;
                    }
                } catch(NullPointerException npe) {
                    // stupid deflater not supporting asynchronous ends..
                    throw (IOException) new IOException().initCause(npe);
                }
                
                // Step 4: If we have no data, tell any interested parties to add some.
                if(incoming.position() == 0) {
                    WriteObserver interested = observer;
                    if(interested != null)
                        interested.handleWrite();
                    
                    // If still no data after that, we've written everything we want -- exit.
                    if (incoming.position() == 0) {
                        // We have nothing left to write, however, it is possible
                        // that between the above check for interested.handleWrite & here,
                        // we got pre-empted and another thread turned on interest.
                        synchronized (this) {
                            if (observer == null) // no observer? good, we can turn interest off
                                source.interestWrite(this, false);
                            // else, we've got nothing to write, but our observer might.
                        }
                        return false;
                    }
                }

                // Step 5: We've got new data to deflate.
                try {
                    deflater.setInput(incoming.array(), 0, incoming.position());
                } catch(NullPointerException npe) {
                    // stupid deflater not supporting asynchronous ends..
                    throw (IOException) new IOException().initCause(npe);
                }
                incoming.clear();
                sync = 0;
            }
        }
    }
    
    /** Shuts down the last observer. */
    public void shutdown() {
        Shutdownable listener = observer;
        if(listener != null)
            listener.shutdown();
    }
    
    /** Unused, Unsupported. */
    public void handleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }

    public boolean hasBufferedOutput() {
        InterestWritableByteChannel channel = this.channel;
        return incoming.position() > 0 || outgoing.hasRemaining() || (channel != null && channel.hasBufferedOutput());
    }
    
}