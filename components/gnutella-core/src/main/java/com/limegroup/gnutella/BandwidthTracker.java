package com.limegroup.gnutella;

/**
 * Defines the interface for any class wanting to track
 * bandwidth. Typically a timer periodically calls {@link #measureBandwidth()},
 * leaving other threads free to call {@link #getMeasuredBandwidth()}.
 */
public interface BandwidthTracker {
    //TODO: you could have measureBandwidth take a time as an argument.

    /** 
     * Measures the data throughput since the last call to <code>measureBandwidth</code>. 
     * This value can be read by calling <code>getMeasuredBandwidth</code>.
     */
    public void measureBandwidth();

    /**
     * Returns the throughput of this in kilobytes/sec (KB/s) between the last
     * two calls to <code>measureBandwidth</code>, or 0.0 if unknown.  
     */
    public float getMeasuredBandwidth() throws InsufficientDataException;
    
    /**
     * Returns the overall averaged bandwidth between 
     * all calls of <code>measureBandwidth</code>.
     */
    public float getAverageBandwidth();
}

