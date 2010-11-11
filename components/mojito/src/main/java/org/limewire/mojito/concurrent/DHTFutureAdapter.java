package org.limewire.mojito.concurrent;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;

/**
 * Instances of {@link DHTFutureAdapter} are using a predefined event
 * {@link Thread}.
 * 
 * @see EventListener
 * @see BlockingEvent
 */
public abstract class DHTFutureAdapter<V> implements EventListener<FutureEvent<V>> {

    @BlockingEvent(queueName="MojitoEventThread")
    @Override
    public final void handleEvent(FutureEvent<V> event) {
        operationComplete(event);
    }
    
    /**
     * @see EventListener#handleEvent(Object)
     */
    protected abstract void operationComplete(FutureEvent<V> event);
}
