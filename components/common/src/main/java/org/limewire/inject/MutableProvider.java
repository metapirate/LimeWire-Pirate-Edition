package org.limewire.inject;

import com.google.inject.Provider;

/** An extension to {@link Provider} that allows the value to change. */
public interface MutableProvider<T> extends Provider<T> {
    
    /**
     * Sets the new value for this setting.  Calling <code>set(T newValue)</code>
     * is necessary for modifications to <code>T</code> to be persisted.  
     * @param newValue
     */
    public void set(T newValue);

}
