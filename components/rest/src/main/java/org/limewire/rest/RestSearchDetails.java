package org.limewire.rest;

import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;

/**
 * Implementation of SearchDetails for REST search requests.
 */
class RestSearchDetails implements SearchDetails {

    private final String query;
    private final SearchCategory searchCategory;
    private final SearchType searchType;
    private final Map<FilePropertyKey, String> advancedSearch;
    
    /**
     * Constructs a new RestSearchDetails with the specified parameters.
     */
    private RestSearchDetails(String query, SearchCategory searchCategory, 
            SearchType searchType, Map<FilePropertyKey, String> advancedSearch) {
        this.query = query;
        this.searchCategory = searchCategory;
        this.searchType = searchType;
        this.advancedSearch = advancedSearch;
    }
    
    /**
     * Returns a new instance of RestSearchDetails using the specified search
     * query.
     */
    public static RestSearchDetails createKeywordSearch(String query) {
        return new RestSearchDetails(query, SearchCategory.ALL, SearchType.KEYWORD, 
                Collections.<FilePropertyKey, String>emptyMap());
    }
    
    @Override
    public Map<FilePropertyKey, String> getAdvancedDetails() {
        return advancedSearch;
    }

    @Override
    public SearchCategory getSearchCategory() {
        return searchCategory;
    }

    @Override
    public String getSearchQuery() {
        return query;
    }

    @Override
    public SearchType getSearchType() {
        return searchType;
    }
}
