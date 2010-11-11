package org.limewire.ui.swing.library;

import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.warnings.LibraryWarningController;

import com.google.inject.Inject;
import com.google.inject.Provider;

class LibraryTransferHandler extends LocalFileListTransferHandler {
    private final Provider<LocalFileList> selectedLocalFileList;
    private final Provider<List<File>> selectedLibraryFiles;

    @Inject
    public LibraryTransferHandler(@LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            @LibrarySelected Provider<List<File>> selectedLibraryFiles, LibraryWarningController librarySupport) {
        super(librarySupport);
        this.selectedLocalFileList = selectedLocalFileList;
        this.selectedLibraryFiles = selectedLibraryFiles;
    }

    @Override
    public LocalFileList getLocalFileList() {
        return selectedLocalFileList.get();
    }
    
    @Override
    protected List<File> getSelectedFiles() {
        return selectedLibraryFiles.get();
    }
}