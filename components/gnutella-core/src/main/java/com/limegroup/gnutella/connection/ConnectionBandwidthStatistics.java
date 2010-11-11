package com.limegroup.gnutella.connection;

import org.limewire.nio.ProtocolBandwidthTracker;

/**
 * Defines the interface that allows bandwidth statistics of a
 * {@link Connection} to be queried.
 */
public interface ConnectionBandwidthStatistics {

    /** Sets how this ConnectionBandwidthStatistics will track SSL. */
    public void setTlsOption(boolean useTls, ProtocolBandwidthTracker sslBandwidthTracker);

    /** Sets how this will track deflate/inflate options. */
    public void setCompressionOption(boolean writeDeflated, boolean readDeflated,
            ProtocolBandwidthTracker compressionBandwidthTracker);

    /** Sets how this will track the raw bandwidth. */
    public void setRawBandwidthTracker(ProtocolBandwidthTracker rawBandwidthTracker);

    /** Returns the percentage lost from incoming SSL transformations. */
    public float getReadLostFromSSL();

    /** Returns the percentage lost from outgoing SSL transformations. */
    public float getSentLostFromSSL();

    /**
     * Returns the percentage saved from having the incoming data compressed.
     */
    public float getReadSavedFromCompression();

    /**
     * Returns the percentage saved through compressing the outgoing data. The
     * value may be slightly off until the output stream is flushed, because the
     * value of the compressed bytes is not calculated until then.
     */
    public float getSentSavedFromCompression();

    /**
     * Returns the number of uncompressed bytes read on this connection. This is
     * equal to the size of all messages received through this connection.
     */
    public long getUncompressedBytesReceived();

    /**
     * Returns the number of bytes received on this connection. If SSL is
     * enabled, this number includes the overhead of incoming SSL wrapped
     * messages. This value will be reduced if compression is enabled for
     * receiving on this connection.
     */
    public long getBytesReceived();

    /**
     * Returns the number of uncompressed bytes sent on this connection. This is
     * equal to the size of the number of messages sent on this connection.
     */
    public long getUncompressedBytesSent();

    /**
     * Returns the number of bytes sent on this connection. If SSL is enabled,
     * this number includes the overhead of the SSL wrapping. This value will be
     * reduced if compression is enabled for sending on this connection.
     */
    public long getBytesSent();

}