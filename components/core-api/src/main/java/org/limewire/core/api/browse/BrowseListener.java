package org.limewire.core.api.browse;

import org.limewire.core.api.search.SearchResult;

/**
 * Listener for a {@link Browse}. As the host is browsed the
 * {@link BrowseListener#handleBrowseResult(SearchResult)} method is called for
 * each file browsed. When the browse is finished the
 * {@link BrowseListener#browseFinished(boolean)} method is called.
 */
public interface BrowseListener {
    /**
     * Called for each file handled while browsing the host.
     */
    void handleBrowseResult(SearchResult searchResult);

    /**
     * Called when the {@link Browse} is finished.
     * 
     * @param success true when the browse completed successfully, false otherwise
     */
    void browseFinished(boolean success);
}
