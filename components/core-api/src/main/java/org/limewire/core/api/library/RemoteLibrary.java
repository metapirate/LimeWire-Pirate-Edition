package org.limewire.core.api.library;

import java.util.Collection;
import java.util.Iterator;

import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.ListenerSupport;

/** An iterable collection of files from a remote host */
public interface RemoteLibrary extends Iterable<SearchResult>, ListenerSupport<RemoteLibraryEvent> {
        
    /**
     * Returns the current state of this friend library. This is a calculated
     * value of all sub-presence libraries. If any sub-library is loading, this
     * returns loading. Otherwise, if one is loaded, this returns loaded.
     * Otherwise, it assumes all have failed and returns failed.
     */
    RemoteLibraryState getState();

    /** Adds a new file into the list. */
    void addNewResult(SearchResult file);
    
    /** Sets all files in the list to be this collection of files. */
    void setNewResults(Collection<SearchResult> files);
    
    /** The size of the remote library. */
    int size();
    
    /** Clears all items out of the remote library. */
    void clear();
    
    /**
     * The iterator that is returned is inherently thread safe without locking.
     * It returns the search results in the order they were added. If the remote
     * library is cleared while an iterator is active, it will return false for
     * the next call of {@link Iterator#hasNext()}.
     */
    @Override
    public Iterator<SearchResult> iterator();
}
