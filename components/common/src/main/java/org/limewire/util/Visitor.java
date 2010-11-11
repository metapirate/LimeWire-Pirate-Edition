package org.limewire.util;

/**
 * A simple visitor.
 * 
 * @param <T> the type of the instances we visit.
 */
public interface Visitor<T> {

    /**
     * Visits <code>value</code> and returns <code>true</code> if we continue, <code>false</code> if we stop.
     * 
     * @param value value to visit
     * @return <code>true</code> if we continue, <code>false</code> if we stop.
     */
    boolean visit(T value);
}
