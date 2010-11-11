package org.limewire.listener;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.util.ExceptionUtils;

public class PendingEventMulticasterImpl<E> implements EventMulticaster<E>, PendingEventBroadcaster<E> {

    private final EventMulticaster<E> multicaster;
    private final ConcurrentLinkedQueue<E> queuedEvents = new ConcurrentLinkedQueue<E>();
    private final AtomicBoolean firing = new AtomicBoolean();
    
    public PendingEventMulticasterImpl() {
        this(new EventMulticasterImpl<E>());
    }
    
    public PendingEventMulticasterImpl(EventMulticaster<E> multicaster) {
        this.multicaster = multicaster;
    }

    @Override
    public void addListener(EventListener<E> eventListener) {
        multicaster.addListener(eventListener);
    }

    @Override
    public boolean removeListener(EventListener<E> eventListener) {
        return multicaster.removeListener(eventListener);
    }
    
    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }
    
    @Override
    public void broadcast(E event) {
        addPendingEvent(event);
        firePendingEvents();
    }

    @Override
    public void addPendingEvent(E event) {
        queuedEvents.add(event);
    }

    @Override
    public void firePendingEvents() {
        // It's possible that an event
        // was queued and another thread called firePendingEvents
        // before after we finished polling queuedEvents
        // but before firing was set back to false. To allow
        // the queued event to fire, we loop and exit if
        // someone else is firing. This guarantees that atleast
        // one thread will actively be sending queued events until
        // no queued events remain.
        while (!queuedEvents.isEmpty()) {
            if (firing.compareAndSet(false, true)) {
                try {
                    Throwable t = null;
                    E e;
                    while ((e = queuedEvents.poll()) != null) {
                        try {
                            multicaster.broadcast(e);
                        } catch(Throwable thrown) {
                            thrown = ExceptionUtils.reportOrReturn(thrown);
                            if(thrown != null && t == null) {
                                t = thrown;
                            }
                        }
                    }
                    if(t != null) {
                        ExceptionUtils.reportOrRethrow(t);
                    }
                } finally {
                    firing.set(false);
                }
            } else {
                // Exit while loop, allow firing thread
                // to take control of broadcasting the pending events.
                break;
            }
        }
    }

    @Override
    public EventListenerList.EventListenerListContext getListenerContext() {
        return multicaster.getListenerContext();
    }
}
