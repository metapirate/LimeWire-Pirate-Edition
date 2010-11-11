package org.limewire.nio.channel;

import org.limewire.nio.observer.WriteObserver;

/**
 * Defines an interface that allows {@link InterestWritableByteChannel 
 * InterestWritableByteChannels} to be set as the target writing channel 
 * for this object.
 * <p>
 * The general flow for chained writing goes something as follows.
 * <p>
 * Install a chain:
 * <pre>
 *      ChannelWriter a = new ProtocolWriter();
 *      ChannelWriter b = new Obfuscator();
 *      ChannelWriter c = new DataDeflater();
 *      a.setWriteChannel(b);
 *      b.setWriteChannel(c);
 *      MyNIOMultiplexor.setWriteObserver(a);
 * </pre>
 * When writing can happen on the socket, the {@link NIOMultiplexor} notifies its
 * internal source that a write can happen. That source will notify the last
 * chain that was interested in it (generally 'c' above, the deflator).
 * 'c' can choose to either pass the event to the last chain that was interested
 * in it (generally 'b') or to instead write data directly to its source.
 * It would opt to write to the source in the case where data was already deflated,
 * and could opt to propagate the event if there was still room to write and someone
 * was interested in getting the event.
 */
public interface ChannelWriter extends WriteObserver {
    
    /**
     * Set the write target, AKA sink. This object should immediately
     * register interest with the newChannel if there is any data to be
     * written.
     */
    void setWriteChannel(InterestWritableByteChannel newChannel);
    
    /** Returns the write target, AKA sink. */
    InterestWritableByteChannel getWriteChannel();
}