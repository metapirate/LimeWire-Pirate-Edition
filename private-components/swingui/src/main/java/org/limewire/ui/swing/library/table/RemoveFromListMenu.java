package org.limewire.ui.swing.library.table;

import java.awt.Dimension;
import java.io.File;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Constructs a menu list of all LocalFileLists excluding the Library.
 * Selecting a given LocalFileList will remove the given files from that
 * list. 
 */
public class RemoveFromListMenu extends JMenu {
   
    /**
     * Constructs an RemoveFromListMenu with all lists enable..
     */
    @Inject
    public RemoveFromListMenu(final @LibrarySelected Provider<List<File>> selectedFiles, 
            final RemoveFromLibraryAction removeFromLibraryAction, 
            final RemoveFromAllListAction removeFromAllAction, final SharedFileListManager manager) {
        super(I18n.tr("Remove from"));     
        
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = RemoveFromListMenu.this;
                menu.removeAll();
                
                removeFromAllAction.putValue(Action.NAME, I18n.tr("All Lists"));
                menu.add(removeFromAllAction).setEnabled(isAllRemovedEnabled(manager, selectedFiles));
                menu.addSeparator();
                menu.add(removeFromLibraryAction);
            }
            
        });
        
        getPopupMenu().addPopupMenuListener(new PopupMenuListener(){
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                //keeps menu from expaning too far
                if(getPopupMenu().getPreferredSize().width > 300)
                    getPopupMenu().setPreferredSize(new Dimension(300, getPopupMenu().getPreferredSize().height));
            }
            
        });
        // place holder to get the -> on the parent menu
        add(new JMenuItem(I18n.tr("empty")));
    }
    
    /**
     * Returns true if a selected file exists in a playlist, false otherwise.
     */
    private boolean isAllRemovedEnabled(SharedFileListManager manager, Provider<List<File>> selectedFiles) {
        boolean enabled = false;
        if(selectedFiles.get().size() == 1) {
            manager.getModel().getReadWriteLock().readLock().lock();
            try { 
                for(SharedFileList fileList : manager.getModel()) {
                    boolean isEnabled = fileList.contains(selectedFiles.get().get(0));
                    enabled |= isEnabled;
                }
            } finally {
                manager.getModel().getReadWriteLock().readLock().unlock();
            }
        } else {
            enabled = true;
        }        
        return enabled;
    }
}
