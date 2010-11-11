package org.limewire.ui.swing.warnings;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class acts as a go between for adding files from the ui to a fileList.
 * It has the correct logic for deciding whether or not a user needs to be
 * warned before a folder drop occurs.
 */
public class LibraryWarningController {
    private final Provider<LibraryWarningDialog> libraryWarningPanel;
    private final LibraryFileAdder libraryFileAdder;
    
    @Inject
    public LibraryWarningController(Provider<LibraryWarningDialog> libraryCategoryWarning,
            LibraryFileAdder libraryFileAdder) {
        this.libraryWarningPanel = libraryCategoryWarning;
        this.libraryFileAdder = libraryFileAdder;
    }

    public void addFiles(final LocalFileList fileList, final List<File> files) {

        int directoryCount = 0;
        for (File file : files) {
            if (fileList.isDirectoryAllowed(file)) {
                directoryCount++;
                if (directoryCount > 1) {
                    // short circuit just need to know if there is more than 1
                    break;
                }
            }
        }

        if (directoryCount > 0) {
            LibraryWarningDialog panel = libraryWarningPanel.get();
            panel.initialize(fileList, files);
        } else {
            // Only files -- add them all.
            libraryFileAdder.addFilesInner(fileList, files, new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return true;
                }
            });
        }
    }
}
