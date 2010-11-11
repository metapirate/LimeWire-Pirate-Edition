package org.limewire.listener;

/**
 * An EventBroadcaster that broadcasts all its events within an executor.
 * This is useful if you want to asynchronously notify listeners about events
 * (especially handy if the events are generated while holding a lock).
 * If the executor is single-threaded, this can guarantee that all events
 * are dispatched in the same order they were broadcast.
 */
public interface AsynchronousEventBroadcaster<E> extends EventBroadcaster<E> {
}
