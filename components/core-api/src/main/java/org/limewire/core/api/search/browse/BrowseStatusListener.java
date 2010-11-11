package org.limewire.core.api.search.browse;

/** Fired when the status of the BrowseSearch changes.  Events fired are non-EDT.*/ 
public interface BrowseStatusListener {
    /**
     * Non-EDT!!!
     * 
     * @param status the new status of the BrowseSearch
     */
    void statusChanged(BrowseStatus status);
}
