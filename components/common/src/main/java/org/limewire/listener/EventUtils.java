package org.limewire.listener;

public class EventUtils {

    /**
     * @return the source of the last event of an event bean or null if the 
     * last event is null or the source is null.
     */
    public static final <S, E extends SourcedEvent<S>> S getSource(EventBean<E> eventBean) {
        SourcedEvent<S> lastEvent = eventBean.getLastEvent();
        return lastEvent != null ? lastEvent.getSource() : null;
    }
    
    /**
     * @return the type of the last event of an event bean or null if the 
     * last event is null or the type is null.
     */
    public static final <T, E extends TypedEvent<T>> T getType(EventBean<E> eventBean) {
        TypedEvent<T> lastEvent = eventBean.getLastEvent();
        return lastEvent != null ? lastEvent.getType() : null;
    }
}
