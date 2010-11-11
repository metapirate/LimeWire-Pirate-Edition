package org.limewire.ui.swing.search.model;

/** A listener that is informed when a new search result is constructed. */
interface VisualSearchResultStatusListener {
    
    /** Notification that a result is added. */
    void resultCreated(VisualSearchResult vsr);
    
    /**
     * Notification that a result has changed.
     * propertyName, oldValue & newValue represent the specific changes.
     */
    void resultChanged(VisualSearchResult vsr, String propertyName, Object oldValue, Object newValue);
    
    /** Notificatino that results have all been cleared. */
    void resultsCleared();
}