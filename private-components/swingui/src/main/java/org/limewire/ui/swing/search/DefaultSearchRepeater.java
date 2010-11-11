package org.limewire.ui.swing.search;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Encapsulates logic for refreshing a Search
 */
public class DefaultSearchRepeater implements SearchRepeater {
    
    private Search search;
    private SearchResultsModel searchResultsModel;

    public DefaultSearchRepeater(Search search, SearchResultsModel searchResultsModel){
        this.search = search;
        this.searchResultsModel = searchResultsModel;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.search.ISearchRepeater#refresh()
     */
    public void refresh(){
        searchResultsModel.clear();
        search.repeat();
    }

}
