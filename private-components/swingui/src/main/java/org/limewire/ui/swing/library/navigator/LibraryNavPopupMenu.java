package org.limewire.ui.swing.library.navigator;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.limewire.ui.swing.library.AddFileAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class LibraryNavPopupMenu extends JPopupMenu {

    private final AddFileAction addAction;
    private final ImportListAction importAction;
    private final ExportListAction exportAction;
    private final Provider<RenameAction> renameAction;
    private final ClearAction clearAction;
    private final Provider<DeleteListAction> deleteAction;
    
    @Inject
    public LibraryNavPopupMenu(LibraryNavigatorPanel navPanel, 
            AddFileAction addAction, ImportListAction importAction,
            ExportListAction exportAction, Provider<RenameAction> renameAction,
            ClearAction clearAction, Provider<DeleteListAction> deleteAction) {
        this.addAction = addAction;
        this.importAction = importAction;
        this.exportAction = exportAction;
        this.renameAction = renameAction;
        this.clearAction = clearAction;
        this.deleteAction = deleteAction;
        
        this.addAction.putValue(Action.NAME, I18n.tr("Add Files..."));

        init(navPanel);
    }
    
    private void init(LibraryNavigatorPanel navPanel) {
        LibraryNavItem item = navPanel.getSelectedNavItem();
        
        add(addAction);
        addSeparator();
        add(importAction);
        add(exportAction);
        addSeparator();
        if(item.getType() == NavType.LIBRARY || item.getType() == NavType.PUBLIC_SHARED) {
            add(clearAction).setEnabled(item.getLocalFileList().size() > 0);
        } else {
            add(renameAction.get());
            addSeparator();
            add(clearAction).setEnabled(item.getLocalFileList().size() > 0);
            add(deleteAction.get()).setEnabled(item.getType() == NavType.LIST && navPanel.getPrivateListCount() > 1);
        }
    }
}
