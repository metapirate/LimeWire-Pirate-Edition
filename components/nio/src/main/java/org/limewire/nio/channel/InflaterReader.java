package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;




/**
 * Reads data from a source channel and offers the inflated version for reading.
 * <p>
 * Each invocation of {@link #read(ByteBuffer)} attempts to return as much
 * inflated data as possible.
 */
public class InflaterReader implements ChannelReader, InterestReadableByteChannel {
    
    /** the inflater that will do the decompressing for us */
    private Inflater inflater;
    
    /** the channel this reads from */
    private InterestReadableByteChannel channel;
    
    /** the temporary buffer that data from the channel goes to prior to inflating */
    private ByteBuffer data;
    
    /**
     * Constructs a new InflaterReader without an underlying source.
     * <p>
     * Prior to <code>read(ByteBuffer)</code> being called, 
     * <code>setReadChannel(ReadableByteChannel)</code> MUST be called.
     */
    public InflaterReader(Inflater inflater) {
        this(null, inflater);
    }
    
    /**
     * Constructs a new <code>InflaterReader</code> with the given source 
     * channel and inflater.
     */
    public InflaterReader(InterestReadableByteChannel channel, Inflater inflater ) {        
        if(inflater == null)
            throw new NullPointerException("null inflater!");

        this.channel = channel;
        this.inflater = inflater;
        this.data = ByteBuffer.allocate(512);
    }
    
    /**
     * Sets the new channel.
     */
    public void setReadChannel(InterestReadableByteChannel channel) {
        if(channel == null)
            throw new NullPointerException("cannot set null channel!");

        this.channel = channel;
    }
    
    /** Gets the read channel. */
    public InterestReadableByteChannel getReadChannel() {
        return channel;
    }
    
    public void interestRead(boolean status) {
        channel.interestRead(status);
    }
    
    /**
     * Reads from this inflater into the given <code>ByteBuffer</code>. 
     * <p>
     * If data isn't available for inflation, data will be read from the source
     * channel and inflation will be attempted. The {@link ByteBuffer} will be
     * filled as much as possible without blocking.
     * <p>
     * The source channel may not be entirely emptied out in a single call to
     * <code>read(ByteBuffer)</code>, because the supplied
     * <code>ByteBuffer</code> may not be large enough to accept all inflated
     * data. If this is the case, the data will remain in the source channel
     * until further calls to <code>read(ByteBuffer)</code>.
     * <p>
     * The source channel does not need to be set for construction. However,
     * before <code>read(ByteBuffer)</code> is called,
     * {@link #setReadChannel(InterestReadableByteChannel)} must be called with
     * a valid channel.
     */
    public int read(ByteBuffer buffer) throws IOException {
        int written = 0;
        int read = 0;
        
        // inflate loop... inflate -> read -> lather -> rinse -> repeat as necessary.
        // only break out of this loop if 
        // a) output buffer gets full
        // b) inflater finishes or needs a dictionary
        // c) no data can be inflated & no data can be read off the channel
        while(buffer.hasRemaining()) { // (case a above)
            // first try to inflate any prior input from the inflater.
            int inflated = inflate(buffer);
            written += inflated;
            
            // if we couldn't inflate anything...
            if(inflated == 0) {
                // if this inflater is done or needs a dictionary, we're screwed. (case b above)
                if (inflater.finished() || inflater.needsDictionary()) {
                    read = -1;
                    break;
                }
            
                // if the buffer needs input, add it.
                if(inflater.needsInput()) {
                    // First gobble up any data from the channel we're dependent on.
                    while(data.hasRemaining() && (read = channel.read(data)) > 0);
                    // if we couldn't read any data, we suck. (case c above)
                    if(data.position() == 0)
                        break;
                    
                    // Then put that data into the inflater.
                    inflater.setInput(data.array(), 0, data.position());
                    data.clear();
                }
            }
            
            // if we're here, we either:
            // a) inflated some data
            // b) didn't inflate, but read some data that we input'd to the inflater
            
            // if a), we'll continue trying to inflate so long as the output buffer
            // has space left.
            // if b), we try to inflate and ultimately end up at a).
        }        
        
        if(written > 0)
            return written;
        else if(read == -1)
            return -1;
        else
            return 0;
    }
    
    /** Inflates data to this buffer. */
    private int inflate(ByteBuffer buffer) throws IOException {
        int written = 0;
        
        int position = buffer.position();
        try {
            written = inflater.inflate(buffer.array(), position, buffer.remaining());
        } catch(DataFormatException dfe) {
            IOException x = new IOException();
            x.initCause(dfe);
            throw x;
        } catch(NullPointerException npe) {
            // possible if the inflater was closed on a separate thread.
            IOException x = new IOException();
            x.initCause(npe);
            throw x;
        }
            
        buffer.position(position + written);
        
        return written;
    }
    
    /**
     * Determines if the underlying channel is open. <code>setReadChannel</code>
     * must be called prior to calling <code>isOpen</code>.
     */
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    /**
     * Closes the underlying channel.
     */
    public void close() throws IOException {
        channel.close();
    }
}