package org.limewire.core.impl.monitor;

/**
 * Defines a list of registered IncomingSearchListener objects. 
 */
public interface IncomingSearchListenerList {

    /**
     * Adds a listener to the list that is notified when incoming query 
     * text is received.  
     */
    public void addIncomingSearchListener(IncomingSearchListener listener);

    /**
     * Removes a listener from the list that is notified when incoming query 
     * text is received.  
     */
    public void removeIncomingSearchListener(IncomingSearchListener listener);

}
