package org.limewire.ui.swing.library.sharing;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class CancelSharingAction extends AbstractAction {

    private final Provider<LibrarySharingPanel> librarySharingPanel;
    
    @Inject
    public CancelSharingAction(Provider<LibrarySharingPanel> librarySharingPanel) {
        super(I18n.tr("Cancel"));
        
        this.librarySharingPanel = librarySharingPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        librarySharingPanel.get().showFriendListView();
    }
}
