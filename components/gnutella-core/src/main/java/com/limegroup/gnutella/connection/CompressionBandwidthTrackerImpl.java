package com.limegroup.gnutella.connection;

import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.limewire.nio.ProtocolBandwidthTracker;

/**
 * A {@link ProtocolBandwidthTracker} that keeps track of the bandwidth
 * statistics used by an {@link Inflater} and {@link Deflater}. It is allowed
 * for either the inflater or deflater to be null, although the methods
 * associated with that particular object can throw a
 * <code>NullPointerException</code>.
 */
public class CompressionBandwidthTrackerImpl implements ProtocolBandwidthTracker {

    private final Inflater inflater;

    private final Deflater deflater;

    public CompressionBandwidthTrackerImpl(Inflater inflater, Deflater deflater) {
        this.inflater = inflater;
        this.deflater = deflater;
    }

    public long getWrittenBytesConsumed() {
        try {
            return deflater.getTotalIn();
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    public long getWrittenBytesProduced() {
        try {
            return deflater.getTotalOut();
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    public long getReadBytesConsumed() {
        try {
            return inflater.getTotalIn();
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    public long getReadBytesProduced() {
        try {
            return inflater.getTotalOut();
        } catch (NullPointerException npe) {
            return 0;
        }
    }

}
