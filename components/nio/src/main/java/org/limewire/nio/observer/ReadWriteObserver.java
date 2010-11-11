package org.limewire.nio.observer;

/**
 * Defines the interface that unifies the <code>ReadObserver</code> and 
 * <code>WriteObserver</code> interfaces. Therefore, one object can be passed
 * around and marked as supporting both read and write handling events.
 */
public interface ReadWriteObserver extends ReadObserver, WriteObserver {}
    
    