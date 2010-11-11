package org.limewire.mojito.concurrent;

import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.AsyncValueFuture;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.util.EventUtils;

/**
 * 
 */
public class DHTValueFuture<V> extends AsyncValueFuture<V> implements DHTFuture<V> {

    /**
     * Creates a {@link DHTValueFuture}
     */
    public DHTValueFuture() {
    }
    
    /**
     * Creates a {@link DHTValueFuture} with the given value
     */
    public DHTValueFuture(V value) {
        setValue(value);
    }
    
    /**
     * Creates a {@link DHTValueFuture} with the given {@link Throwable}
     */
    public DHTValueFuture(Throwable exception) {
        setException(exception);
    }

    @Override
    public long getTimeout(TimeUnit unit) {
        return 0L;
    }

    @Override
    public long getTimeoutInMillis() {
        return getTimeout(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isTimeout() {
        return false;
    }
    
    @Override
    protected boolean isEventThread() {
        return EventUtils.isEventThread();
    }
    
    @Override
    protected void fireOperationComplete(
            final EventListener<FutureEvent<V>>[] listeners,
            final FutureEvent<V> event) {
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DHTValueFuture.super.fireOperationComplete(listeners, event);
            }
        };
        
        EventUtils.fireEvent(task);
    }
}
