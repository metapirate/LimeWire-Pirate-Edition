package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all files that this library is managing.
 * This list can include files that are shared, are not shared,
 * are files from the store, are shared with friends, are incomplete, etc...
 * 
 * Inclusion in this list means only that LimeWire knows about this file.
 */
public interface Library extends FileCollection {
    
    void addManagedListStatusListener(EventListener<LibraryStatusEvent> listener);
    void removeManagedListStatusListener(EventListener<LibraryStatusEvent> listener);
    
    void addPropertyChangeListener(PropertyChangeListener listener);
    void removePropertyChangeListener(PropertyChangeListener listener);
  
    void addFileProcessingListener(EventListener<FileProcessingEvent> listener);    
    void removeFileProcessingListener(EventListener<FileProcessingEvent> listener);
    
    /** Returns true if the initial load of the library has finished. */
    boolean isLoadFinished();
    
    /** Informs the library that the file 'oldName' has been renamed to 'newName'. */
    ListeningFuture<FileDesc> fileRenamed(File oldName, File newName);
    
    /** Informs the library that the file 'file' has changed. */
    ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs);
    
    /** Returns true if this is allowed to many any programs. */
    boolean isProgramManagingAllowed();
    
    /** Cancels any pending file tasks. */
    void cancelPendingTasks();
    
    /** Returns the number of files in the share list directly from the raw library db */
    int peekPublicSharedListCount();
}
