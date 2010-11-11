package org.limewire.ui.swing.filter;

import org.limewire.core.api.search.SearchDetails.SearchType;

/**
 * Defines a factory for creating the advanced filter panel.
 */
public interface AdvancedFilterPanelFactory<E extends FilterableItem> {

    /**
     * Creates a new AdvancedFilterPanel using the specified filterable data
     * source.
     */
    public AdvancedFilterPanel<E> create(FilterableSource<E> filterableSource, SearchType type);
    
}
