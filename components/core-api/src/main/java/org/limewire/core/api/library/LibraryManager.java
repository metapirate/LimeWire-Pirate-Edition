package org.limewire.core.api.library;

import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

/** Exposes information about the library. */
public interface LibraryManager {
    
    /** Returns the list from which all local library data is stored. */
    LibraryFileList getLibraryManagedList();
    
    /** Returns an object from which more data about the library can be queried. */
    LibraryData getLibraryData();

    /**
     * Returns the ListEventPublisher that must be used when constructing any
     * EventList that will sync with any library list.
     */
    ListEventPublisher getLibraryListEventPublisher();

    /**
     * Returns the ReadWriteLock that must be used when constructing any
     * EventList that will sync with a library list.
     */
    ReadWriteLock getReadWriteLock();

}
