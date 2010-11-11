package org.limewire.listener;

/** 
 * An analog to {@link EventBroadcaster} for when it is unwise to fire events
 * immediately.  This is particularly useful when events need to be fired in 
 * a particular order, but it is unwise to fire them immediately because the
 * broadcaster is holding a lock.  (It is unsafe to fire callbacks within
 * a lock, as it can easily lead to deadlock.)
 */
// duplicate of asynchronous broadcaster ???
public interface PendingEventBroadcaster<E> {
    
    /** Adds an event that will be fired the next time {@link #firePendingEvents()} is called. */ 
    void addPendingEvent(E event);
    
    /** Fires all pending events, in the order they were added. */
    void firePendingEvents();

}
