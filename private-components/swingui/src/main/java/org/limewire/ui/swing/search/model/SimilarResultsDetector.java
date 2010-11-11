package org.limewire.ui.swing.search.model;

/**
 * Responsible for detecting a VisualSearchResult that is similar to another
 * result and associating the two. Internally the results detector will maintain
 * a cache of all results that came before the given result for matching. It is
 * responsible for building its own index and figuring out what to do from
 * there. It also updates the properties of the visual search result to mark the
 * parents and children when results are found. Visibility is updated to
 * properly represent the end states as well when new parents are selected.
 */
public interface SimilarResultsDetector {

    void detectSimilarResult(VisualSearchResult result);

    void removeSpamItem(VisualSearchResult visualSearchResult, VisualSearchResult newParent);
    
    void clear();

}
