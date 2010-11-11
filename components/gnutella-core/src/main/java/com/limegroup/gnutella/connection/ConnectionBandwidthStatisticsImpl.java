package com.limegroup.gnutella.connection;

import org.limewire.nio.ProtocolBandwidthTracker;

/**
 * Default implementation for retrieving statistics about the bandwidth of
 * different {@link ProtocolBandwidthTracker ProtocolBandwidthTrackers}.
 * <p>
 * 
 * In order for statistics to be calculated and retrieved correctly, the
 * available <code>set</code> methods must be called to link up the tracking
 * and inform <code>ConnectionBandwidthStatisticsImpl</code> that tracking is
 * requested.
 */
public class ConnectionBandwidthStatisticsImpl implements ConnectionBandwidthStatistics {

    private volatile boolean useTls;

    private volatile ProtocolBandwidthTracker sslBandwidthTracker;

    private volatile boolean writeDeflated;

    private volatile boolean readDeflated;

    private volatile ProtocolBandwidthTracker compressionBandwidthTracker;

    private volatile ProtocolBandwidthTracker rawBandwidthTracker;

    public void setTlsOption(boolean useTls, ProtocolBandwidthTracker sslBandwidthTracker) {
        this.useTls = useTls;
        this.sslBandwidthTracker = sslBandwidthTracker;
    }

    public void setCompressionOption(boolean writeDeflated, boolean readDeflated,
            ProtocolBandwidthTracker compressionBandwidthTracker) {
        this.writeDeflated = writeDeflated;
        this.readDeflated = readDeflated;
        this.compressionBandwidthTracker = compressionBandwidthTracker;
    }

    public void setRawBandwidthTracker(ProtocolBandwidthTracker rawBandwidthTracker) {
        this.rawBandwidthTracker = rawBandwidthTracker;
    }

    /**
     * Returns the number of bytes sent on this connection. If SSL is enabled,
     * this number includes the overhead of the SSL wrapping. This value will be
     * reduced if compression is enabled for sending on this connection.
     */
    public long getBytesSent() {
        if (useTls)
            return sslBandwidthTracker.getWrittenBytesProduced();
        else if (writeDeflated)
            return compressionBandwidthTracker.getWrittenBytesProduced();
        else
            return rawBandwidthTracker.getWrittenBytesProduced();
    }

    /**
     * Returns the number of uncompressed bytes sent on this connection. This is
     * equal to the size of the number of messages sent on this connection.
     */
    public long getUncompressedBytesSent() {
        return rawBandwidthTracker.getWrittenBytesProduced();
    }

    /**
     * Returns the number of bytes received on this connection. If SSL is
     * enabled, this number includes the overhead of incoming SSL wrapped
     * messages. This value will be reduced if compression is enabled for
     * receiving on this connection.
     */
    public long getBytesReceived() {
        if (useTls)
            return sslBandwidthTracker.getReadBytesConsumed();
        else if (readDeflated)
            return compressionBandwidthTracker.getReadBytesConsumed();
        else
            return rawBandwidthTracker.getReadBytesConsumed();
    }

    /**
     * Returns the number of uncompressed bytes read on this connection. This is
     * equal to the size of all messages received through this connection.
     */
    public long getUncompressedBytesReceived() {
        return rawBandwidthTracker.getReadBytesConsumed();
    }

    /**
     * Returns the percentage saved through compressing the outgoing data. The
     * value may be slightly off until the output stream is flushed, because the
     * value of the compressed bytes is not calculated until then.
     */
    public float getSentSavedFromCompression() {
        return getWriteSavings(writeDeflated, compressionBandwidthTracker);
    }

    /**
     * Returns the percentage saved from having the incoming data compressed.
     */
    public float getReadSavedFromCompression() {
        return getReadSavings(readDeflated, compressionBandwidthTracker);
    }

    /** Returns the percentage lost from outgoing SSL transformations. */
    public float getSentLostFromSSL() {
        return getWriteLosings(useTls, sslBandwidthTracker);
    }

    /** Returns the percentage lost from incoming SSL transformations. */
    public float getReadLostFromSSL() {
        return getReadLosings(useTls, sslBandwidthTracker);
    }

    /**
     * Returns the percentage of bandwidth saved from using the given protocol
     * for writing, if guard is true.
     */
    private static float getWriteSavings(boolean guard, ProtocolBandwidthTracker tracker) {
        if (!guard) {
            return 0;
        } else {
            long writtenConsumed = tracker.getWrittenBytesConsumed();
            if (writtenConsumed != 0) {
                return 1 - ((float) tracker.getWrittenBytesProduced() / (float) writtenConsumed);
            } else {
                return 0;
            }
        }
    }

    /**
     * Returns the percentage of bandwidth saved from using the given protocol
     * for reading, if guard is true.
     */
    private static float getReadSavings(boolean guard, ProtocolBandwidthTracker tracker) {
        if (!guard) {
            return 0;
        } else {
            long readProduced = tracker.getReadBytesProduced();
            if (readProduced != 0) {
                return 1 - ((float) tracker.getReadBytesConsumed() / (float) readProduced);
            } else {
                return 0;
            }
        }
    }

    /**
     * Returns the percentage of bandwidth lost from using the given protocol
     * for writing, if guard is true.
     */
    private static float getWriteLosings(boolean guard, ProtocolBandwidthTracker tracker) {
        if (!guard) {
            return 0;
        } else {
            long writeProduced = tracker.getWrittenBytesProduced();
            if (writeProduced != 0) {
                return 1 - (float) tracker.getWrittenBytesConsumed() / (float) writeProduced;
            } else {
                return 0;
            }
        }
    }

    /**
     * Returns the percentage of bandwidth lost from using the given protocol
     * for reading, if guard is true.
     */
    private static float getReadLosings(boolean guard, ProtocolBandwidthTracker tracker) {
        if (!guard) {
            return 0;
        } else {
            long readConsumed = tracker.getReadBytesConsumed();
            if (readConsumed != 0) {
                return 1 - (float) tracker.getReadBytesProduced() / (float) readConsumed;
            } else {
                return 0;
            }
        }
    }

}
