package org.limewire.inject;

/**
 * Simple default implementation of {@link MutableProvider}.
 * <p>
 * Threadsafe.
 */
public class MutableProviderImpl<T> implements MutableProvider<T> {

    private volatile T value;
    
    public MutableProviderImpl(T initialValue) {
        value = initialValue;
    }
    
    @Override
    public void set(T newValue) {
        value = newValue;
    }

    @Override
    public T get() {
        return value;
    }

}
