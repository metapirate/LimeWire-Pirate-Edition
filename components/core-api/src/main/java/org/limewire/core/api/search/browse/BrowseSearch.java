package org.limewire.core.api.search.browse;

import org.limewire.core.api.search.Search;

/**
 * Handles browse host as a Search.
 */
public interface BrowseSearch extends Search {

    void addBrowseStatusListener(BrowseStatusListener listener);
    
    void removeBrowseStatusListener(BrowseStatusListener listener);

}
