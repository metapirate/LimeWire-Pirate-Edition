package org.limewire.listener;

public class EventRebroadcaster<E> implements EventListener<E> {
    
    private final EventBroadcaster<E> broadcaster;
    
    public EventRebroadcaster(EventBroadcaster<E> broadcaster) {
        this.broadcaster = broadcaster;
    }
    
    @Override
    public void handleEvent(E event) {
        broadcaster.broadcast(event);
    }

}
