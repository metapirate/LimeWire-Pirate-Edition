package org.limewire.concurrent;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * An {@link AsyncFuture} is an {@link ListeningFuture} with the ability
 * to set the result value or {@link Exception} asynchronously.
 * 
 * @see ListeningFuture
 * @see FutureTask
 */
public interface AsyncFuture<V> extends ListeningFuture<V> {

    /**
     * Sets the {@link AsyncFuture}'s value and returns true on success
     */
    public boolean setValue(V value);
    
    /**
     * Sets the {@link AsyncFuture}'s exception and returns true on success
     */
    public boolean setException(Throwable exception);
    
    /**
     * Returns true if the {@link AsyncFuture} completed abnormally 
     * (i.e. {@link #get()} and {@link #get(long, TimeUnit)} will
     * throw an {@link Exception}).
     */
    public boolean isCompletedAbnormally();
}
