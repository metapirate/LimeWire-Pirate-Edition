package org.limewire.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventListenerList.EventListenerListContext;

/**
 * Delegates from one ListeningFuture to another, converting from the Source
 * type to the Result type.
 */
public abstract class ListeningFutureDelegator<S, R> implements ListeningFuture<R> {
    
    private final ListeningFuture<S> delegate;
    private final EventListenerListContext listenerContext;
    
    public ListeningFutureDelegator(ListeningFuture<S> delegate) {
        this.delegate = delegate;
        this.listenerContext = new EventListenerListContext();
    }

    public void addFutureListener(final EventListener<FutureEvent<R>> listener) {
        // If we're done, we can just dispatch immediately w/o having to 
        // worry about creating a delegate listener.
        if(isDone()) {
            EventListenerList.dispatch(listener, FutureEvent.createEvent(this), listenerContext);
        } else {        
            delegate.addFutureListener(new EventListener<FutureEvent<S>>() {
                public void handleEvent(FutureEvent<S> event) {
                    EventListenerList.dispatch(listener, FutureEvent.createEvent(ListeningFutureDelegator.this), listenerContext);
                }
            });
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    public R get() throws InterruptedException, ExecutionException {
        S s;
        try {
            s = delegate.get();
        } catch(ExecutionException ee) {
            return convertException(ee);            
        }
        
        return convertSource(s);
    }

    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        S s;
        try {
            s = delegate.get(timeout, unit);
        } catch(ExecutionException ee) {
            return convertException(ee);            
        }
        
        return convertSource(s);
    }

    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    public boolean isDone() {
        return delegate.isDone();
    }
    
    /** Converts from S to R. If it cannot be converted, throws an ExecutionException. */
    protected abstract R convertSource(S source) throws ExecutionException;
    
    /** Converts from an ExecutionException. If it cannot be converted, rethrows an ExecutionException. */
    protected abstract R convertException(ExecutionException ee) throws ExecutionException;

}
