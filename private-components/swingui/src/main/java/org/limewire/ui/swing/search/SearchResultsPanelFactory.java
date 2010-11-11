package org.limewire.ui.swing.search;

import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Defines a factory for creating the search results display container.
 */
public interface SearchResultsPanelFactory {
    
    /**
     * Creates a new SearchResultsPanel using the specified search results data
     * model.
     */
    public SearchResultsPanel createSearchResultsPanel(
            SearchResultsModel searchResultsModel);
    
}
