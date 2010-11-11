package org.limewire.collection;

import java.util.Iterator;

/** 
 * A convenience class to aid in developing iterators that cannot be modified.
 */
public abstract class UnmodifiableIterator<E> implements Iterator<E> {
    /** Throws <code>UnsupportedOperationException</code>. */
    public final void remove() {
		throw new UnsupportedOperationException();
    }
}
