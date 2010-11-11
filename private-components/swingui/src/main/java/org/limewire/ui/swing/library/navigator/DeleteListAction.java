package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class DeleteListAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    private final Provider<SharedFileListManager> sharedFileListManager;
    
    @Inject
    public DeleteListAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel,
            Provider<SharedFileListManager> sharedFileListManager) {
        super(I18n.tr("Delete"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
        this.sharedFileListManager = sharedFileListManager;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final LibraryNavItem item = libraryNavigatorPanel.get().getSelectedNavItem();
        int confirmation = FocusJOptionPane.showConfirmDialog(null, getMessage(item), I18n.tr("Delete List"), JOptionPane.OK_CANCEL_OPTION); 
        if (confirmation == JOptionPane.OK_OPTION) {
            sharedFileListManager.get().deleteSharedFileList((SharedFileList)item.getLocalFileList());
        }
    }
    
    private String getMessage(LibraryNavItem nav) {
        switch(nav.getType()) {    
        case LIST:        
            if (nav.isShared()){            
                return I18n.tr("Delete list {0}?  This will stop sharing all of these files, but won't delete them from disk.", nav.getDisplayText());
            } else {
                return I18n.tr("Delete list {0}?", nav.getDisplayText());                
            }
        case PUBLIC_SHARED:  
            if (nav.isShared()){            
                return I18n.tr("Delete list {0}?  This will stop sharing all of these files with the world, but won't delete them from disk.", nav.getDisplayText());
            } else {          
                return I18n.tr("Delete list {0}?", nav.getDisplayText());
            }
        default:        
            throw new IllegalStateException("unknown type: " + nav.getType());    
        }
    }
}
