package org.limewire.concurrent;

import com.google.inject.Provider;

/**
 * Provides a reference to an object that is created when first
 * needed. An abstract class, <code>AbstractLazySingletonProvider</code> includes an 
 * implementation to retrieve an object T. You must
 * implement {@link #createObject()} in a subclass.
 * 
 * 
 * For more 
 * information see <a href="http://en.wikipedia.org/wiki/Lazy_initialization">
 * Lazy initialization</a>.
 */
public abstract class AbstractLazySingletonProvider<T> implements Provider<T> {
    
    /** The backing object. */
    private T obj;
    
    /** Whether or not construction has already started. */
    private boolean constructing;

    /** Retrieves the reference, creating it if necessary. */
    public synchronized T get() {
        if(obj == null) {
            if(constructing)
                throw new IllegalStateException("constructing again!");
            constructing = true;
            obj = createObject();
        }
        return obj;
    }
    
    /** Creates the object this reference will use. */
    protected abstract T createObject();

}
