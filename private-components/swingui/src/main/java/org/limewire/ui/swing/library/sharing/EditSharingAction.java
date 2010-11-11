package org.limewire.ui.swing.library.sharing;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Switches the sharing view to the editable view.
 */
class EditSharingAction extends AbstractAction {

    private final Provider<LibrarySharingPanel> librarySharingPanel;
    
    @Inject
    public EditSharingAction(Provider<LibrarySharingPanel> librarySharingPanel) {
        this.librarySharingPanel = librarySharingPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        librarySharingPanel.get().showEditableView();
    }
}
