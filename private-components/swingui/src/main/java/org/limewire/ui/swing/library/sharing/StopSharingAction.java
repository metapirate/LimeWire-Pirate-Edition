package org.limewire.ui.swing.library.sharing;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.google.inject.Inject;

/** An action that will stop sharing the current selected list with all its friends. */
class StopSharingAction extends AbstractAction {
    
    private final LibrarySharingPanel panel;
    
    @Inject public StopSharingAction(LibrarySharingPanel panel) {
        this.panel = panel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        panel.stopSharing();
    }

}
