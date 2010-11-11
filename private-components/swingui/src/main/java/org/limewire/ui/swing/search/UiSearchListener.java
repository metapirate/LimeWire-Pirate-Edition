package org.limewire.ui.swing.search;

/** A listener that is notified when a search is triggered. */
public interface UiSearchListener {
    
    /** Notification that a new search is requested. */
    public void searchTriggered(SearchInfo searchInfo);

}
