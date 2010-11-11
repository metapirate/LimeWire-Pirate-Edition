package org.limewire.listener;


/**
 * An alternate interface to ListenerSupport that allows listeners to 
 * be added according to a specific source.
 */
public interface SourcedListenerSupport<E extends SourcedEvent<S>, S> {
    
    /** Adds a listener for this specific source. */
    public void addListener(S source, EventListener<E> listener);
    
    /** Removes a listener for this specific source. */
    public boolean removeListener(S source, EventListener<E> listener);
    
    /** Removes all listeners for a given source. */
    public boolean removeListeners(S source);

}
