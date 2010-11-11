package org.limewire.rudp;


/**
 * Defines the interface of settings to control the RUDP algorithm.
 * Currently only supports the ACK skipping algorithm.
 */
public interface RUDPSettings {

    /** Returns true if ack-skipping is enabled. */
    public boolean isSkipAcksEnabled();
    
    /** Returns the maximum number of acks to skip. */
    public int getMaxSkipAcks();
   
    /** Returns the deviation for skipping acks. */
    public float getMaxSkipDeviation();
   
    /** Returns the length of periods for skipping acks. */
    public int getSkipAckPeriodLength();
   
    /** Returns the size of the history remembered for skipping acks. */
    public int getSkipAckHistorySize();

}
