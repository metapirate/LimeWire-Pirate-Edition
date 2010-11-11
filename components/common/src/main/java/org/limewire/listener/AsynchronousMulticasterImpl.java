package org.limewire.listener;

import java.util.concurrent.Executor;

import org.limewire.listener.EventListenerList.EventListenerListContext;
import org.limewire.logging.Log;

public class AsynchronousMulticasterImpl<E> implements EventMulticaster<E>, AsynchronousEventMulticaster<E> {
    
    private final EventListenerList<E> listeners;
    private final Executor executor;
    
    public AsynchronousMulticasterImpl(Executor executor) {
        this.listeners = new EventListenerList<E>();
        this.executor = executor;
    }
    
    public AsynchronousMulticasterImpl(Executor executor, Class loggerKey) {
        this.listeners = new EventListenerList<E>(loggerKey);
        this.executor = executor;
    }
    
    public AsynchronousMulticasterImpl(Executor executor, Log log) {
        this.listeners = new EventListenerList<E>(log);
        this.executor = executor;
    }

    public EventListenerListContext getListenerContext() {
        return listeners.getContext();
    }    

    @Override
    public void addListener(EventListener<E> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<E> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }

    @Override
    public void broadcast(final E event) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listeners.broadcast(event);
            }
        });
    }
}
