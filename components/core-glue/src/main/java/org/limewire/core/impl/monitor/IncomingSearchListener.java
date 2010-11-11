package org.limewire.core.impl.monitor;

/**
 * Defines a listener to handle incoming query text.
 */
public interface IncomingSearchListener {

    /**
     * Adds the specified query text to the list.  
     */
    public void handleQueryString(String query);
    
}
