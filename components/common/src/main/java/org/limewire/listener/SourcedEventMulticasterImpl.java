package org.limewire.listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.listener.EventListenerList.EventListenerListContext;

/** A default implementation of {@link SourcedEventMulticaster}. */
public class SourcedEventMulticasterImpl<E extends SourcedEvent<S>, S> implements
        SourcedEventMulticaster<E, S> {

    /** The context all listeners will use. */
    private final EventListenerListContext listenerContext;

    /** The list of listeners for every change event. */
    private final EventListenerList<E> listenersForAll;
    
    /** A Map of listeners for each source. */
    private final Map<S, EventListenerList<E>> sourceListeners;
    
    public SourcedEventMulticasterImpl() {
        this(new EventListenerListContext());
    }
    
    public SourcedEventMulticasterImpl(EventListenerListContext context) {
        this.listenerContext = context;
        this.listenersForAll = new EventListenerList<E>(listenerContext);
        this.sourceListeners = new ConcurrentHashMap<S, EventListenerList<E>>();
    }
    
    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }
    
    @Override
    public void broadcast(E event) {
        listenersForAll.broadcast(event);
        // No lock is necessary here, because a ConcurrentHashMap is used,
        // so it is thread-safe as far as retrieval goes.
        EventListenerList<E> list = sourceListeners.get(event.getSource());
        if(list != null) {
            list.broadcast(event);
        }        
    }
    
    @Override
    public void addListener(EventListener<E> listener) {
        listenersForAll.addListener(listener);
    }

    @Override
    public void addListener(S source, EventListener<E> listener) {
        // This is locked on sourceListeners to prevent race conditions
        // where #removeListener(S, EventListener<E>) removes
        // the list between getting & inserting.
        synchronized(sourceListeners) {
            EventListenerList<E> list = sourceListeners.get(source);
            if(list == null) {
                list = new EventListenerList<E>(listenerContext);
                sourceListeners.put(source, list);
            }
            list.addListener(listener);
        }
    }

    @Override
    public boolean removeListener(EventListener<E> listener) {
        return listenersForAll.removeListener(listener);
    }

    @Override
    public boolean removeListener(S source, EventListener<E> listener) {
        // This is locked on sourceListeners to prevent race conditions
        // where #addListener(S, EventListener<E>) adds a listener
        // between checking for the size & removal.
        synchronized(sourceListeners) {
            EventListenerList<E> list = sourceListeners.get(source);
            if(list == null) {
                return false;
            } else {
                boolean removed = list.removeListener(listener);
                if(list.size() == 0) {
                    sourceListeners.remove(source);
                }
                return removed;
            }
        }
    }
    
    @Override
    public boolean removeListeners(S source) {
        synchronized(sourceListeners) {
            return sourceListeners.remove(source) != null;
        }
    }
}
