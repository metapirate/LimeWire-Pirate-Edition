package org.limewire.core.api.monitor;

import ca.odell.glazedlists.EventList;

/**
 * Defines the manager interface for incoming searches.
 */
public interface IncomingSearchManager {

    /**
     * Returns the list of incoming search phrases.
     */
    public EventList<String> getIncomingSearchList();
    
    /**
     * Sets an indicator to enable the list.
     */
    public void setListEnabled(boolean enabled);
    
    /**
     * Sets the size of the list. 
     */
    public void setListSize(int size);
    
}
