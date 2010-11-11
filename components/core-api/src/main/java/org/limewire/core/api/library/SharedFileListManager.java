package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

import ca.odell.glazedlists.EventList;

/**
 * Manager for all share lists.
 */
public interface SharedFileListManager {
    
    String SHARED_FILE_COUNT = "sharedFileCount";
    
    /**
     * Adds a {@link PropertyChangeListener}. Any listeners added while on the
     * EDT thread will also be notified on the EDT thread, regardless of the
     * thread the event originates on.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /** Removes the {@link PropertyChangeListener}. */
    void removePropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * Gets the current total number of files that are shared with anyone.
     * This list strips out duplicates -- a file shared with two people
     * or a file shared via two collections will only be counted once.
     */
    int getSharedFileCount();

    /**
     * Returns an EventList that can be used to view all {@link SharedFileList
     * SharedFileLists}.
     */
    EventList<SharedFileList> getModel();

    /**
     * Creates a new {@link SharedFileList} of the appropriate name. The new
     * list will be reflected in {@link #getModel()} once the core processes it.
     * Returns the id of the new list
     */
    int createNewSharedFileList(String name);
    
    /** Deletes the SharedFileList with this name. */
    void deleteSharedFileList(SharedFileList fileList);

    
    /**
     * Removes all documents from all public share lists, and turns off sharing documents with gnutella. 
     */
    void removeDocumentsFromPublicLists();
 
}
