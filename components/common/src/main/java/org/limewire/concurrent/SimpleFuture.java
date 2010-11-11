package org.limewire.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventListenerList.EventListenerListContext;

/** A future that is designed to return what is passed into its constructor. */
public class SimpleFuture<T> implements ListeningFuture<T> {
    
    private final T t;
    private final ExecutionException exception;
    private final EventListenerListContext context = new EventListenerListContext();
    
    public SimpleFuture(T t) {
        this.t = t;
        this.exception = null;
    }
    
    public SimpleFuture(Throwable throwable) {
        this.t = null;
        this.exception = new ExecutionException(throwable);
    }
    
    public SimpleFuture(ExecutionException ee) {
        this.t = null;
        this.exception = ee;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public T get() throws ExecutionException {
        if(exception != null) {
            throw exception;
        } else {
            return t;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        if(exception != null) {
            throw exception;
        } else {
            return t;
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }
    
    @Override
    public void addFutureListener(EventListener<FutureEvent<T>> listener) {
        EventListenerList.dispatch(listener, FutureEvent.createEvent(this), context);
    }

}
