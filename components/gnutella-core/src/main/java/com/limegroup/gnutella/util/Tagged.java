package com.limegroup.gnutella.util;

/**
 * An Object with a value and a tag whether the value is valid or not.
 */
public final class Tagged<T> {

    private final T value;
    private final boolean isValid;
    
    public Tagged(T value, boolean isValid) {
        this.value = value;
        this.isValid = isValid;
    }
    
    public T getValue() {
        return value;
    }
    
    public boolean isValid() {
        return isValid;
    }
}
