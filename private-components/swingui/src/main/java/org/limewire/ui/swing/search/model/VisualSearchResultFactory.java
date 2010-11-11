package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.GroupedSearchResult;

/**
 * Defines a factory to create instances of VisualSearchResult.
 */
public interface VisualSearchResultFactory {
    
    VisualSearchResult create(GroupedSearchResult gsr, VisualSearchResultStatusListener listener);
}
