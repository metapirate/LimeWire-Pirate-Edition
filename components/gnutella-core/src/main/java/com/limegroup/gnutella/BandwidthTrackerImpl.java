package com.limegroup.gnutella;

import org.limewire.collection.Buffer;


/**
 * A helper class for implementing the BandwidthTracker interface
 */
public class BandwidthTrackerImpl {
    private static final int HISTORY_SIZE=10;

    /** Keep 10 clicks worth of data, which we can then average to get a more
     *  accurate moving time average.
     *  INVARIANT: snapShots[0]==measuredBandwidth.floatValue() */
    private final Buffer<Float> snapShots = new Buffer<Float>(HISTORY_SIZE);
    
    /**
     * Number of times we've been bandwidth measured.
     */
    private int numMeasures = 0;
    
    /**
     * Overall average throughput
     */
    private float averageBandwidth = 0;
    
    /**
     * The cached getMeasuredBandwidth value.
     */
    private float cachedBandwidth = 0;
    
    private long lastTime;

    /** The most recent measured bandwidth.  DO NOT DELETE THIS; it exists
     *  for backwards serialization reasons. */
    private float measuredBandwidth;

    /** The last amount read */
    private long lastAmountRead64;
    
    /** 
     * Measures the data throughput since the last call to measureBandwidth,
     * assuming this has read amountRead bytes.  This value can be read by
     * calling getMeasuredBandwidth.  
     *
     * @param amountRead the cumulative amount read from this, in BYTES.
     *  Should be larger than the argument passed in the last call to
     *  measureBandwidth(..).
     */
    public synchronized void measureBandwidth(long amountRead) {
        long currentTime=System.currentTimeMillis();
        //We always discard the first sample, and any others until after
        //progress is made.  
        //This prevents sudden bandwidth spikes when resuming
        //uploads and downloads.  Remember that bytes/msec=KB/sec.
        if (lastAmountRead64==0 || currentTime==lastTime) {
            measuredBandwidth=0.f;
        } else {            
            measuredBandwidth=(float)(amountRead-lastAmountRead64)
                                / (float)(currentTime-lastTime);
            //Ensure positive!
            measuredBandwidth=Math.max(measuredBandwidth, 0.f);
        }
        lastTime=currentTime;
        lastAmountRead64=amountRead;
        averageBandwidth = (averageBandwidth*numMeasures + measuredBandwidth)
                            / ++numMeasures;
        snapShots.add(new Float(measuredBandwidth));
        cachedBandwidth = 0;
    }

    /** @see BandwidthTracker#getMeasuredBandwidth */
    public synchronized float getMeasuredBandwidth() 
        throws InsufficientDataException {
        if(cachedBandwidth != 0)
            return cachedBandwidth;

        int size = snapShots.getSize();
        if (size  < 3 )
            throw new InsufficientDataException();
        float total = 0;
        for(Float f : snapShots)
            total += f.floatValue();
        cachedBandwidth = total/size;
        return cachedBandwidth;
    }
    
    /**
     * Returns the average overall bandwidth consumed.
     */
    public synchronized float getAverageBandwidth() {
        if(snapShots.getSize() < 3) return 0f;
        return averageBandwidth;
    }
}
