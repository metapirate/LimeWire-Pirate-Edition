package org.limewire.concurrent;

import java.util.concurrent.ScheduledFuture;

/**
 * An extension to {@link ScheduledFuture} where you can add listeners.
 * If the future has completed when the listener is added,
 * the listener is immediately notified.
 * 
 * @see ListeningExecutorService
 * @see ScheduledListeningExecutorService
 * @see ScheduledListeningFuture
 * @see RunnableListeningFuture
 * @see RunnableScheduledListeningFuture
 */
public interface ScheduledListeningFuture<V> extends ScheduledFuture<V>, ListeningFuture<V> {

}
