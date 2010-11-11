package org.limewire.ui.swing.filter;

import org.limewire.core.api.search.SearchCategory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Defines a source for filterable data.  Known implementations include
 * {@link org.limewire.ui.swing.search.model.SearchResultsModel SearchResultsModel}.
 */
public interface FilterableSource<E extends FilterableItem> {

    /**
     * Returns the filter category.
     */
    SearchCategory getFilterCategory();
    
    /**
     * Returns the filter debugger.
     */
    FilterDebugger<E> getFilterDebugger();
    
    /**
     * Returns an unfiltered list of items.
     */
    EventList<E> getUnfilteredList();

    /**
     * Returns a filtered list of items.
     */
    EventList<E> getFilteredList();
    
    /**
     * Sets the MatcherEditor used to filter items. 
     */
    void setFilterEditor(MatcherEditor<E> editor);
    
}
