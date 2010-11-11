package org.limewire.listener;

/** Allows one to rebroadcast events to any listeners with events based on a SourcedEvent. */
public interface SourcedEventMulticaster<E extends SourcedEvent<S>, S> extends
        SourcedListenerSupport<E, S>, ListenerSupport<E>, EventListener<E>, EventBroadcaster<E> {
    
    
    
}
