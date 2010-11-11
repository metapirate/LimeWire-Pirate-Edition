package org.limewire.nio;


/**
 * Defines a throttle interface; a throttle is a mechanism that controls
 * the flow of data.
 */
public interface Throttle {
    
    /**
     * Interests this listener in receiving a bandwidthAvailable callback.
     */
    public void interest(ThrottleListener writer);
    
    /**
     * Requests some data for writing from this Throttle.
     */
    public int request();
    
    /**
     * Releases some unwritten requested data back to the throttle.
     */
    public void release(int amount);
    
    /**
     * Sets the rate of the throttle in bytes / second.
     */
    public void setRate(float rate);
    
    /**
     * @return the time in milliseconds when the next tick will begin.
     */
    public long nextTickTime();
}
    