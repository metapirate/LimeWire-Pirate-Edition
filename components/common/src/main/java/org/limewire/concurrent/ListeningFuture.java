package org.limewire.concurrent;

import java.util.concurrent.Future;

import org.limewire.listener.EventListener;

/**
 * An extension to Future where you can add listeners.
 * If the future has completed when the listener is added,
 * the listener is immediately notified.
 * 
 * @see ListeningExecutorService
 * @see ScheduledListeningExecutorService
 * @see ScheduledListeningFuture
 * @see RunnableListeningFuture
 * @see RunnableScheduledListeningFuture
 */
public interface ListeningFuture<V> extends Future<V> {
    
    /**
     * Adds a listener that will be notified when the future is finished.
     * If the future has finished prior to this being called, it is immediately
     * notified.
     */
    public void addFutureListener(EventListener<FutureEvent<V>> listener);

}
