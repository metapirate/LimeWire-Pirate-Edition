package org.limewire.ui.swing.library.table;

import java.io.File;
import java.util.List;

import javax.swing.JMenu;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.library.LibraryPanel;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Factory class to create menus to work with lists.
 */
public class ListMenuFactory {

    private final SharedFileListManager sharedFileListManager; 
    private final LibraryManager libraryManager;
    private final LibraryPanel libraryPanel;
    
    /**
     * Constructs a ListMenuFactory.
     */
    @Inject
    public ListMenuFactory(SharedFileListManager sharedFileListManager, 
            LibraryManager libraryManager, LibraryPanel libraryPanel) {
        this.sharedFileListManager = sharedFileListManager;
        this.libraryManager = libraryManager;
        this.libraryPanel = libraryPanel;
    }
    
    /**
     * Creates an "Add to List" menu for the specified list of selected files.
     */
    public JMenu createAddToListMenu(Provider<List<File>> selectedFiles) {
        AddToListMenu menu = new AddToListMenu(selectedFiles);
        menu.initialize(sharedFileListManager);
        return menu;
    }
    
    /**
     * Creates a "Show in List" menu for the specified list of selected files.
     */
    public JMenu createShowInListMenu(Provider<List<File>> selectedFiles, boolean showLibrary) {
        ShowInListMenu menu = new ShowInListMenu(selectedFiles);
        menu.initialize(sharedFileListManager, libraryManager, libraryPanel, showLibrary);
        return menu;
    }
    
    /**
     * Creates a "Show in List" menu for the specified list of selected files
     * and local file list.
     */
    public JMenu createShowInListMenu(Provider<List<File>> selectedFiles,
            Provider<LocalFileList> selectedLocalFileList, boolean showLibrary) {
        ShowInListMenu menu = new ShowInListMenu(selectedFiles, selectedLocalFileList);
        menu.initialize(sharedFileListManager, libraryManager, libraryPanel, showLibrary);
        return menu;
    }
}
