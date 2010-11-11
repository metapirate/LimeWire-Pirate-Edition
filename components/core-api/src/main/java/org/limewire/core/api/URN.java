package org.limewire.core.api;

/**
 * Exposes the Underlying URN for testing equality of files only.
 * The real urn is hidden from non core code.
 */
public interface URN extends Comparable<URN> {

    public boolean equals(Object obj);
    
    public int hashCode();
    
    public String toString();
}
