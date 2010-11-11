package org.limewire.nio;

/**
 * Defines an interface to allow implementations to query the produced/consumed
 * statistics of an arbitrary protocol. Depending on the nature of the protocol,
 * produced/consumed numbers may either shrink or increase. For example, if this
 * were used to retrieve the data from SSL transformations, the amount of read
 * bytes produced would be smaller than the amount of read bytes consumed
 * (because of the overhead of the SSL packet), whereas if this were used to
 * retrieve data about compression, the amount of read bytes produced would be
 * greater than the amount of bytes produced (because the data was shrunk and
 * then expanded).
 */
public interface ProtocolBandwidthTracker {

    /**
     * Returns the total number of bytes that this has produced after
     * transforming incoming data.
     */
    public long getReadBytesProduced();

    /**
     * Returns the total number of bytes that this has consumed while reading
     * incoming data.
     */
    public long getReadBytesConsumed();

    /**
     * Returns the total number of bytes that this has produced after
     * transforming outgoing data.
     */
    public long getWrittenBytesProduced();

    /**
     * Returns the total number of bytes that this has consumed while writing
     * outgoing data.
     */
    public long getWrittenBytesConsumed();

}
