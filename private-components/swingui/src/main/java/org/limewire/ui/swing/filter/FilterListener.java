package org.limewire.ui.swing.filter;

/**
 * Defines a listener to handle filter changes.
 */
public interface FilterListener<E extends FilterableItem> {

    /**
     * Invoked when the filter changes in the specified filter component.
     */
    void filterChanged(Filter<E> filter);
    
}
