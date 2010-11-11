package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class RenameAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    
    @Inject
    public RenameAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel) {
        super(I18n.tr("Rename"));

        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        SharedFileList sharedFileList = (SharedFileList)libraryNavigatorPanel.get().getSelectedNavItem().getLocalFileList();
        libraryNavigatorPanel.get().editSharedListName(sharedFileList);
    }
}
