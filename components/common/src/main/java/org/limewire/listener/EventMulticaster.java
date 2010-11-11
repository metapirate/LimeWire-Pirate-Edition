package org.limewire.listener;

/**
 * Describes the interface for which all incoming events can be forwarded to any registered listeners.
 */
public interface EventMulticaster<E> extends ListenerSupport<E>, EventListener<E>, EventBroadcaster<E> {

    /**
     * @return A context object that allows for proper invocation of listeners
     * accross multiple events.
     */
    EventListenerList.EventListenerListContext getListenerContext();
}
