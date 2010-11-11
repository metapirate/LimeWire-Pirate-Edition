package org.limewire.listener;

/**
 * A bean-like interface for an event.
 * This exposes the event via {@link #getLastEvent()} method.
 * This is typically useful for implementations that do not wish
 * to listen to events, but instead want to occasionally poll
 * what the last event was.
 */
public interface EventBean<E> {
    
    /** Returns the most recent event that was broadcast. */
    E getLastEvent();

}
