package org.limewire.core.impl.monitor;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.monitor.IncomingSearchManager;
import org.limewire.inject.EagerSingleton;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

/**
 * An implementation of <code>IncomingSearchManager</code> for the live core.
 */
@EagerSingleton
public class CoreIncomingSearchManager 
    implements IncomingSearchManager, IncomingSearchListener {

    /** List of incoming search phrases. */
    private final EventList<String> incomingSearchList;
    /** Indicator to enable list. */
    private boolean listEnabled = true;
    /** Number of list elements. */
    private int listSize = 32;
    
    /**
     * Constructs the live implementation of IncomingSearchManager using the
     * specified incoming search listener list.
     */
    @Inject
    public CoreIncomingSearchManager(IncomingSearchListenerList incomingListenerList) {
        
        // Create list of incoming search text.
        incomingSearchList = GlazedListsFactory.threadSafeList(
                new BasicEventList<String>());
        
        // Add manager as listener for incoming search events. 
        incomingListenerList.addIncomingSearchListener(this);
    }

    /**
     * Returns a read-only list of incoming search phrases.  An application 
     * should NOT assume that the returned list is Swing-compatible; Swing is 
     * supported by wrapping the resulting list via a call to <code>
     * GlazedListsFactory.swingThreadProxyEventList()</code>.
     */
    @Override
    public EventList<String> getIncomingSearchList() {
        return GlazedListsFactory.readOnlyList(incomingSearchList);
    }

    /**
     * Sets an indicator to enable the list.
     */
    @Override
    public void setListEnabled(boolean enabled) {
        incomingSearchList.getReadWriteLock().writeLock().lock();
        try {
            listEnabled = enabled;
        } finally {
            incomingSearchList.getReadWriteLock().writeLock().unlock();
        }
    }
    
    /**
     * Sets the size of the list. 
     */
    @Override
    public void setListSize(int size) {
        incomingSearchList.getReadWriteLock().writeLock().lock();
        try {
            // Set size, and reduce list to fit.
            listSize = size;
            reduceList(size);
        } finally {
            incomingSearchList.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Adds the specified query text to the list.  
     */
    @Override
    public void handleQueryString(String query) {
        incomingSearchList.getReadWriteLock().writeLock().lock();
        try {
            // Reduce list and add query text if enabled.
            if (listEnabled) {
                incomingSearchList.add(0, query);
                reduceList(listSize);
            }
        } finally {
            incomingSearchList.getReadWriteLock().writeLock().unlock();
        }
    }
    
    /**
     * Reduces the incoming search list to the specified size.  Elements are
     * removed from the end of the list.
     */
    private void reduceList(int size) {
        // Reduce list to fit specified size.
        while ((size >= 0) && (incomingSearchList.size() > size)) {
            incomingSearchList.remove(incomingSearchList.size() - 1);
        }
    }
}
