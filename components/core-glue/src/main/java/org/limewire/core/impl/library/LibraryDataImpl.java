package org.limewire.core.impl.library;

import java.io.File;

import org.limewire.core.api.library.LibraryData;

import com.google.inject.Inject;
import com.limegroup.gnutella.library.Library;

class LibraryDataImpl implements LibraryData {

    private final Library library;
    
    @Inject
    public LibraryDataImpl(Library fileList) {
        this.library = fileList;
    }
    
    @Override
    public boolean isFileManageable(File f) {
        return library.isFileAllowed(f);
    }
    
    @Override
    public boolean isProgramManagingAllowed() {
        return library.isProgramManagingAllowed();
    }

    @Override
    public boolean isDirectoryAllowed(File folder) {
        return library.isDirectoryAllowed(folder);
    }

    @Override
    public int peekPublicSharedListCount() {
        return library.peekPublicSharedListCount();
    }
}