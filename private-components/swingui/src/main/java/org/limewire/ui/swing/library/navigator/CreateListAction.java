package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Creates a new PlayList with the name "Untitled", selects
 * that PlayList and then enables editing on it.
 */
public class CreateListAction extends AbstractAction {

    private final Provider<SharedFileListManager> shareManager;
    private final Provider<LibraryNavigatorTable> navTable;
    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    
    @Inject
    public CreateListAction(Provider<SharedFileListManager> shareManager,
            Provider<LibraryNavigatorTable> navTable,
            Provider<LibraryNavigatorPanel> libraryNavigatorPanel) {
        super("Create New List");
        
        this.shareManager = shareManager;
        this.navTable = navTable;
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final int id = shareManager.get().createNewSharedFileList(I18n.tr("Untitled"));
        navTable.get().selectLibraryNavItem(id);
        SharedFileList sharedFileList = (SharedFileList)libraryNavigatorPanel.get().getSelectedNavItem().getLocalFileList();
        libraryNavigatorPanel.get().editSharedListName(sharedFileList);
    }
}
