package org.limewire.ui.swing.search;

import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Defines a factory for creating the container for search results tables.
 */
public interface ResultsContainerFactory {

    /**
     * Creates a new ResultsContainer using the specified search results data
     * model.
     */
    public ResultsContainer create(SearchResultsModel searchResultsModel);
    
}
