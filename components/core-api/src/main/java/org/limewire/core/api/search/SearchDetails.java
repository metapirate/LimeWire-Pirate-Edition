package org.limewire.core.api.search;

import java.util.Map;

import org.limewire.core.api.FilePropertyKey;

/**
 * Details about how the search should be constructed.
 */
public interface SearchDetails {
    
    public static enum SearchType {
        KEYWORD(false), WHATS_NEW(false), SINGLE_BROWSE(true), MULTIPLE_BROWSE(true), ALL_FRIENDS_BROWSE(true);
        private final boolean browse;

        SearchType(boolean isBrowse) {
            browse = isBrowse;
        }

        public boolean isBrowse() {
            return browse;
        }
    };
    
    /** What category this search is in. */
    public SearchCategory getSearchCategory();
    
    /** The query that should be sent out for this search. */
    public String getSearchQuery();
    
    /** The kind of search. */
    public SearchType getSearchType();
    
    /** The advanced search map. */
    public Map<FilePropertyKey, String> getAdvancedDetails();
    
    

}
