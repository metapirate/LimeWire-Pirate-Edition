package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;

/**
 * Factory for creating auto-complete dictionaries for queries based on
 * category.
 */
interface SmartAutoCompleteFactory {

    /**
     * Creates an AutoCompleteDictionary for the specified search category.
     */
    SmartAutoCompleteDictionary create(SearchCategory searchCategory);
}
