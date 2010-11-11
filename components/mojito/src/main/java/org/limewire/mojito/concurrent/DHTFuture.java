package org.limewire.mojito.concurrent;

import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.AsyncFuture;

/**
 * 
 */
public interface DHTFuture<V> extends AsyncFuture<V> {
    
    /**
     * Returns the {@link DHTFuture}'s timeout in the given {@link TimeUnit}
     */
    public long getTimeout(TimeUnit unit);
    
    /**
     * Returns the {@link DHTFuture}'s timeout in milliseconds
     */
    public long getTimeoutInMillis();
    
    /**
     * Returns true if the {@link DHTFuture} completed due to a timeout
     */
    public boolean isTimeout();
}
