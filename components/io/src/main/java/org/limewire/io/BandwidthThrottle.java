package org.limewire.io;

import java.util.concurrent.locks.ReentrantLock;

/** 
 * Limits throughput of a stream to at most N bytes per T seconds. 
 * <code>BandwidthThrottle</code> is mutable and thread-safe.
 * <p>
 * In the following example, <code>throttle</code> is used to send the contents
 * of <code>buf</code> to <code>out</code> at no more than <code>N/T</code> 
 * bytes per second:
 * <pre>
 *      BandwidthThrottle throttle=new BandwidthThrottle(N / T);
 *      OutputStream out=...;
 *      byte[] buf=...;
 *      for (int i=0; i < buf.length; ) {
 *          int allowed=throttle.request(buf.length-i);
 *          out.write(buf, i, allowed);
 *          i+=allowed;
 *      }
 * </pre>
 * The <code>BandwidthThrottle</code> class works by allowing exactly N bytes to be
 * sent every T seconds. If the number of bytes for a given dialog is exceeded,
 * subsequent calls to the <code>request</code> method block until bytes become
 * available. 
 * <p>
 * Smaller T values allow fairer bandwidth sharing because many different
 * {@link Thread Threads} can request smaller amounts of bandwidth in parallel, 
 * as opposed to one thread hogging the allocated bandwidth. Unlike fairness as 
 * described in {@link ReentrantLock}, <code>BandwidthThrottle</code> does not 
 * favor the thread that has waited the longest time. 
 * <p>
 * Note that throttles are not cumulative; this may change in the future
 * which could allow for fancier bandwidth control. 
 * <p>
 * <code>BandwidthThrottle</code>'s implementation is based on the 
 * <code>Bandwidth</code> class from the <a href ="http://freenetproject.org/">
 * Freenet Project</a> <code>Bandwidth</code> class. <code>BandwidthThrottle</code> 
 * is a simplified version of the Freenet predecessor.
*/
/*
 * An example of future, fancier controls: 
 * <code>BandwidthThrottle</code> may be able delegate to other
 * throttles to allow, for example, a 15 KB/s Gnutella messaging throttle, with
 * no more than 10 KB/s devoted to uploads.
 */

public class BandwidthThrottle {
    /** The number of windows per second. */
    private static final int TICKS_PER_SECOND = 10;
    /** The value of T, in milliseconds. */
    private static final int MILLIS_PER_TICK = 1000 / TICKS_PER_SECOND;

    /** The bytes to send per tick.  Modified by setThrottle. */
    private volatile int _bytesPerTick; 
    
    /**
     * Whether or not we're only allowing bandwidth to be used every other
     * second.
     */
    private volatile boolean _switching = false;

    /** The number of bytes remaining in this window. */
    private int _availableBytes; 
    /** The system time when the window is reset so more bytes can be sent. */
    private long _nextTickTime; 

    /**
     * Creates a new bandwidth throttle at the given throttle rate.
     * The default windows size T is used. The bytes per windows N
     * is calculated from bytesPerSecond.
     *
     * @param bytesPerSecond the limits in bytes (not bits) per second
     * (not milliseconds)
     */    
    public BandwidthThrottle(float bytesPerSecond) {
        setRate(bytesPerSecond);
    }
    
    /**
     * Creates a new bandwidth throttle at the given throttle rate, 
     * only allowing bandwidth to be used every other second if
     * switching is true.
     * The default windows size T is used. The bytes per windows N
     * is calculated from bytesPerSecond.
     *
     * @param bytesPerSecond the limits in bytes (not bits!) per second
     * (not milliseconds!)
     * @param switching true if we should only allow bandwidth to be used
     *        every other second.
     */    
    public BandwidthThrottle(float bytesPerSecond, boolean switching) {
        setRate(bytesPerSecond);
        setSwitching(switching);

    }    

    /**
     * Sets the throttle to the given throttle rate. The default windows size
     * T is used. The bytes per windows N is calculated from bytesPerSecond.
     *
     * @param bytesPerSecond the limits in bytes (not bits) per second
     * (not milliseconds)  
     */
    public void setRate(float bytesPerSecond) {
        _bytesPerTick = (int)(bytesPerSecond / TICKS_PER_SECOND);
        if(_switching)
            fixBytesPerTick(true);
    }
    
    /**
     * Sets whether or not this throttle is switching bandwidth on/off.
     */
    public void setSwitching(boolean switching) {
        if(_switching != switching)
            fixBytesPerTick(switching);
        _switching = switching;
    }
    
    /**
     * Modifies bytesPerTick to either be double or half of what it was.
     * This is necessary because of the 'switching', which can effectively
     * reduce or raise the amount of data transferred.
     */
    private void fixBytesPerTick(boolean raise) {
        int newBytesPerTick = _bytesPerTick;
        if(raise)
            newBytesPerTick *= 2;
        else
            newBytesPerTick /= 2;
        if(newBytesPerTick < 0) // overflowed?
            newBytesPerTick = Integer.MAX_VALUE;
        _bytesPerTick = newBytesPerTick;
    }

    /**
     * Blocks until the caller can send at least one byte without violating
     * bandwidth constraints. Records the number of byte sent.
     *
     * @param desired the number of bytes the caller would like to send
     * @return the number of bytes the sender is expected to send, which
     *  is always greater than one and less than or equal to desired
     */
    synchronized public int request(int desired) {
        waitForBandwidth();
        int result = Math.min(desired, _availableBytes);
        _availableBytes -= result;
        return result;
    }
    
    /** Waits until data is _availableBytes. */
    private void waitForBandwidth() {
        for (;;) {
            long now = System.currentTimeMillis();
            updateWindow(now);
            if (_availableBytes != 0)
                break;
            try {
                Thread.sleep(_nextTickTime - now);
            } catch (InterruptedException e) {  //TODO: propagate
            }
        }
    }
    
    /** Updates _availableBytes and _nextTickTime if possible.  */
    private void updateWindow(long now) {
        if (now >= _nextTickTime) {
            if(!_switching || ((now/1000)%2)==0) {
                _availableBytes = _bytesPerTick;
                _nextTickTime = now + MILLIS_PER_TICK;
            } else {
                _availableBytes = 0;
                // the next tick time is the time we'll hit
                // the next second.
                long diff = 1000 - (now % 1000);
                _nextTickTime = now + diff;
            }   
        }
    }

    //Tests: see core/com/.../tests/BandwidthThrottleTest
}
