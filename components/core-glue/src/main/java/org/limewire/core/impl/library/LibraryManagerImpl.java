package org.limewire.core.impl.library;


import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;

import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class LibraryManagerImpl implements LibraryManager {
    
    private final LibraryFileListImpl libraryList;
    private final LibraryData libraryData;
    
    @Inject
    public LibraryManagerImpl(LibraryFileListImpl libraryFileListImpl, LibraryData libraryData) {
        this.libraryList = libraryFileListImpl;
        this.libraryData = libraryData;
    }
    
    @Override
    public LibraryFileList getLibraryManagedList() {
        return libraryList;
    }
    
    public LibraryData getLibraryData() {
        return libraryData;
    }

    @Override
    public ListEventPublisher getLibraryListEventPublisher() {
        return libraryList.getModel().getPublisher();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return libraryList.getModel().getReadWriteLock();
    }
}
