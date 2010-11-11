package org.limewire.listener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.OnewayExchanger;

/**
 * Provides a synchronous way to listen to one time events. Makes mostly
 * sense in test scenarios.  
 */
public class BlockingEventListener<E> implements EventListener<E> {

    private final OnewayExchanger<E, RuntimeException> exchanger = new OnewayExchanger<E, RuntimeException>();
    
    @Override
    public void handleEvent(E event) {
        exchanger.setValue(event);
    }
    
    /**
     * Returns last existing event or the next event that is called within the given
     * timeout period. 
     * <p>
     * As a side effect the stored event is also cleared.
     * 
     * @return null if an {@link InterruptedException} occurred or the event didn't
     * occur within given time out period
     */
    public E getEvent(long timeout, TimeUnit units) {
        try {
            synchronized (exchanger) {
                E event = exchanger.get(timeout, units);
                exchanger.reset();
                return event;
            }
        } catch (InterruptedException e) {
            return null;
        } catch (TimeoutException e) {
            return null;
        }
    }

}
