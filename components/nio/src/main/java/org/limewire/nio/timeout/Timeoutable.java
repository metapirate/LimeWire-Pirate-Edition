package org.limewire.nio.timeout;

/** 
 * Defines the interface for an object that can time out. 
 */
public interface Timeoutable {
    public void notifyTimeout(long now, long expired, long timeoutLength);
}
