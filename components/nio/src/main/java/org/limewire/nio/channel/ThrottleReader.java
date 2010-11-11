package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.limewire.nio.RequiresSelectionKeyAttachment;
import org.limewire.nio.Throttle;
import org.limewire.nio.ThrottleListener;
import org.limewire.util.BufferUtils;



/**
 * Reads data from a channel. The data is controlled by a {@link Throttle}; to 
 * work with the <code>Throttle</code>, <code>ThrottleReader</code> uses an 
 * attachment.
 */
public class ThrottleReader implements InterestReadableByteChannel, ChannelReader, RequiresSelectionKeyAttachment {
    
    //private static final Log LOG = LogFactory.getLog(ThrottleReader.class);
    
    /** The channel to write to & interest on. */    
    private volatile InterestReadableByteChannel channel;
    /** The throttle we're using. */
    private final Throttle throttle;
    /** The amount of data we were told we can read. */
    private int available;
    
    /** The last interest state, to interact well with the Throttle. */
    private volatile boolean lastInterestState;
    
    private final Listener throttleListener;
    
    /**
     * Constructs a <code>ThrottleReader</code> with the given 
     * <code>Throttle</code>.
     * <p>
     * <b>NOTE</b>: You must call 
     * {@link #setReadChannel(InterestReadableByteChannel) 
     * setReadChannel(InterestReadableByteChannel)}
     * prior to using <code>ThrottleReader</code>.
     */
    public ThrottleReader(Throttle throttle) {
        this(throttle, null);
    }
    
    /**
     * Constructs a new <code>ThrottleReader</code> with the given throttle and 
     * channel.
     */
    public ThrottleReader(Throttle throttle, InterestReadableByteChannel channel) {
        this.throttle = throttle;
        this.channel = channel;
        this.throttleListener = new Listener();
    }
    
    /** Retrieves the channel. */
    public InterestReadableByteChannel getReadChannel() {
        return channel;
    }
    
    /** Sets the channel. */
    public void setReadChannel(InterestReadableByteChannel channel) {
        this.channel = channel;
        throttle.interest(throttleListener);
    }
        
    /**
     * Tells the <code>Throttle</code> that we're interested in receiving 
     * bandwidthAvailable events at some point in time.
     */
    public void interestRead(boolean status) {
        lastInterestState = status;
        if(channel != null) {
            if(status)
                throttle.interest(throttleListener);
            else
                channel.interestRead(false);
        }
    }
    
    /**
     * Read data from the chain.
     * <p>
     * Only reads up to 'available' amount of data.
     */
    public int read(ByteBuffer buffer) throws IOException {
        InterestReadableByteChannel chain = channel;
        if(chain == null)
            throw new IllegalStateException("reading with no chain!");
            
        
        int totalRead = 0;
        if (available > 0) {
            int priorLimit = buffer.limit();
            if (buffer.remaining() > available) {
                // LOG.debug("Limting amount remaining to read from " +
                // buffer.remaining() + " to " + available);
                buffer.limit(buffer.position() + available);
            }

            try {
                totalRead = channel.read(buffer);
            } finally {
                buffer.limit(priorLimit);
            }

            if (totalRead > 0)
                available -= totalRead;
        } else {
            // Humor the underlying channel -- it may need
            // to read data internally.
            // Important to set totalRead because it may be -1, 
            // which we have to propagate outwards for EOF detection.
            totalRead = channel.read(BufferUtils.getEmptyBuffer());
            channel.interestRead(false);
            if(lastInterestState)
                throttle.interest(throttleListener);
        }
        //LOG.debug("Read: " + totalRead  + ", leaving: " + available + " left.");
        
        return totalRead;
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
     * The attachment must be the same as the attachment of the
     * <code>SelectionKey</code> associated with the socket
     * <code>ThrottleReader</code> uses.
     */
    public void setAttachment(Object o) {
        throttleListener.setAttachment(o);
    }
    
    private final class Listener implements ThrottleListener {
        /** The object that the Throttle will recognize as the SelectionKey attachments */
        private Object attachment;
        
        /**
         * Notification from the <code>Throttle</code> that bandwidth is available.
         * Returns <code>false</code> if the channel no longer is open and will not 
         * be interested again.
         */
        public boolean bandwidthAvailable() {
            if(channel.isOpen() && lastInterestState) {
                channel.interestRead(true);
                return true;
            } else {
                return false;
            }
        }

        /** Determines if the underlying channel is open. */
        public boolean isOpen() {
            return ThrottleReader.this.isOpen();
        }
        
        /**
         * Requests some bandwidth from the throttle.
         */
        public void requestBandwidth() {
            available = throttle.request();
        }
        
        /**
         * Releases available bandwidth back to the throttle.
         */
        public void releaseBandwidth() {
            throttle.release(available);
            available = 0;
            if(lastInterestState)
                throttle.interest(this);
        }

        /** Sets the attachment that the <code>Throttle</code> will recognize for this Writer. */
        public void setAttachment(Object att) {
            attachment = att;
        }
        
        /** Gets the attachment. */
        public Object getAttachment() {
            return attachment;
        }
        
    }
}
