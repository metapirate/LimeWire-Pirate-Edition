package org.limewire.collection;

import java.util.concurrent.Callable;

/**
 * Defines the interface for a <code>Callable</code> implementation that does 
 * not throw an exception.
 */
public interface NECallable<T> extends Callable<T> {
	T call();
}
