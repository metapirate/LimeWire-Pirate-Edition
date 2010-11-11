package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ClearAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    
    @Inject
    public ClearAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel) {
        super(I18n.tr("Clear"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final LibraryNavItem item = libraryNavigatorPanel.get().getSelectedNavItem();
        int confirmation = FocusJOptionPane.showConfirmDialog(null, getMessage(item), I18n.tr("Clear Files"), JOptionPane.OK_CANCEL_OPTION); 
        if (confirmation == JOptionPane.OK_OPTION) {
            item.getLocalFileList().clear();               
        }
    }
    
    private String getMessage(LibraryNavItem nav){
    
        switch(nav.getType()) {    
        case LIST:        
            if (nav.isShared()){            
                return I18n.tr("Remove all files from {0}?  This will stop sharing all of these files.", nav.getDisplayText());
            } else {
                return I18n.tr("Remove all files from {0}?", nav.getDisplayText());                
            }
            case LIBRARY:        
            return I18n.tr("Remove all files from your library?  This will remove all files you've downloaded and stop sharing every file you are sharing.");
        case PUBLIC_SHARED:  
            if (nav.isShared()){            
                return I18n.tr("Remove all files from {0}?  This will stop sharing all of these files with the world.", nav.getDisplayText());
            } else {          
                return I18n.tr("Remove all files from {0}?", nav.getDisplayText());
            }
        default:        
            throw new IllegalStateException("unknown type: " + nav.getType());    
        }
    }
    
}
