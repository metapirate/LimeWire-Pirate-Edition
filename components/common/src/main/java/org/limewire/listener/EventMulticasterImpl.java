package org.limewire.listener;

import org.limewire.listener.EventListenerList.EventListenerListContext;
import org.limewire.logging.Log;

/**
 * An implementation of an event multicaster.
 * This forwards all received events to all listeners.
 */
public class EventMulticasterImpl<E> implements EventMulticaster<E> {
    
    private final EventListenerList<E> listeners;
    
    public EventMulticasterImpl() {
        this.listeners = new EventListenerList<E>();
    }
    
    public EventMulticasterImpl(Class loggerKey) {
        this.listeners = new EventListenerList<E>(loggerKey);
    }
    
    public EventMulticasterImpl(Log log) {
        this.listeners = new EventListenerList<E>(log);
    }
    
    public EventMulticasterImpl(EventListenerListContext context) {
        this.listeners = new EventListenerList<E>(context);
    }
    
    public EventListenerListContext getListenerContext() {
        return listeners.getContext();
    }
    
    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }
    
    @Override
    public void broadcast(E event) {
        listeners.broadcast(event);
    }

    @Override
    public void addListener(EventListener<E> eventListener) {
        listeners.addListener(eventListener);
    }

    @Override
    public boolean removeListener(EventListener<E> eventListener) {
        return listeners.removeListener(eventListener);        
    }
}
