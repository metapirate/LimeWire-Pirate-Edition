package org.limewire.ui.swing.library.table;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.ShareListIcons;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Provider;

class AddToListMenu extends JMenu {

    private final Provider<List<File>> selectedFiles;
    private final ShareListIcons icons = new ShareListIcons();
    
    /**
     * Constructs an AddToListMenu with all lists enable..
     */
    public AddToListMenu(Provider<List<File>> selectedFiles) {
        super(I18n.tr("Add to List"));  

        this.selectedFiles = selectedFiles;
    }
        
        
    public void initialize(final SharedFileListManager manager){        
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = AddToListMenu.this;
                menu.removeAll();
                // once this is selected, show all the submenus
                manager.getModel().getReadWriteLock().readLock().lock();
                try { 
                    for(SharedFileList fileList : manager.getModel()) {
                        if(selectedFiles.get().size() == 1) {
                            menu.add(new AddListAction(fileList.getCollectionName(), icons.getListIcon(fileList), fileList));
                        } else {
                            menu.add(new AddListAction(fileList.getCollectionName(), icons.getListIcon(fileList), fileList));
                        }
                    }
                } finally {
                    manager.getModel().getReadWriteLock().readLock().unlock();
                }
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
    
    
    private class AddListAction extends AbstractAction {
        private final LocalFileList localFileList;
        
        public AddListAction(String text, Icon icon, LocalFileList localFileList) {
            super(text);
            putValue(SMALL_ICON, icon);
            this.localFileList = localFileList;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            BackgroundExecutorService.execute(new Runnable(){
                public void run() {
                    List<File> selected = new ArrayList<File>(selectedFiles.get());
                    for(File file : selected) {
                        localFileList.addFile(file);
                    }                    
                }
            });
        }
    }

}
