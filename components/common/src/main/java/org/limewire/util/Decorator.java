package org.limewire.util;

/** A simple interface that allows something to decorate an object with a different object. */
public interface Decorator<I, O> {
    
    /** Decorates I and returns O. */
    public O decorate(I input);

}
