package com.limegroup.gnutella.statistics;

/**
 * A collection of statistics for out-of-band data.
 */
public interface OutOfBandStatistics {
	    
    /** Adds some number of requested responses. */
    public void addRequestedResponse(int numRequested);
    
    /** Adds some number of bypassed responses. */
    public void addBypassedResponse(int numBypassed);
    
    /** Adds some number of received responses. */
    public void addReceivedResponse(int numReceived);
    
    /** Adds a single sent query. */
    public void addSentQuery();
    
    /** Returns the total number of requested responses. */
    public int getRequestedResponses();
    
    /** Returns the current sample size. */
    public int getSampleSize();
    
    /** Increments the sample size by some amount. */
    public void increaseSampleSize();

    /**
     * @return a double from 0 to 100 that signifies the OOB success percentage.
     */
    public double getSuccessRate();

    /**
     * @return whether or not the success rate is good enough.
     */
    public boolean isSuccessRateGood();

    /**
     * @return whether or not the success rate is good enough for proxying.
     */
    public boolean isSuccessRateGreat();

    /**
     * @return whether or not the success rate is terrible (less than 40%).
     */
    public boolean isSuccessRateTerrible();

    /**
     * @return A boolean if OOB queries have seemed ineffective, i.e. we've
     * sent several but not received ANY results.  Note that this is pessimistic
     * and may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    public boolean isOOBEffectiveForProxy();

    /**
     * @return A boolean if OOB queries have seemed ineffective, i.e. we've
     * sent several but not received ANY results.  Note that this is pessimistic
     * and may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    public boolean isOOBEffectiveForMe();

}
