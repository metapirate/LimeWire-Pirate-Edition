package org.limewire.nio.observer;

/**
 * Defines an interface that marks the class as being able to be shutdown.
 * <p>
 * This interface should release any resources acquired, as well as propagate
 * the shutting down to any components that need to be shutdown.
 */
public interface Shutdownable {
    
    /**
     * Releases any resources used by this component.
     * <p>
     * <code>shutdown</code> must never throw any exceptions.
     */
    void shutdown();
    
}