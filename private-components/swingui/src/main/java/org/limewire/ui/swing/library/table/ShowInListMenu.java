package org.limewire.ui.swing.library.table;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Provider;

class ShowInListMenu extends JMenu {
    
    private final Provider<List<File>> selectedFiles;
    private LibraryPanel libraryPanel;
    private final ShowInListMenuIcons icons = new ShowInListMenuIcons();
    private Provider<LocalFileList> selectedLocalFileList;
    
    /**
     * Constructs a ShowInListMenu with all items enabled.  Initialize must be called after construction (except for injected subclasses).
     */
    public ShowInListMenu(Provider<List<File>> selectedFiles) {
        this(selectedFiles, null);
    }
 
    /**
     * Constructs a ShowInListMenu with all items but selectedLocalFileList enabled.  Initialize must be called after construction (except for injected subclasses).
     */
    public ShowInListMenu(Provider<List<File>> selectedFiles, final Provider<LocalFileList> selectedLocalFileList) {
        super(I18n.tr("Show in List"));        
        
        this.selectedFiles = selectedFiles;
        this.selectedLocalFileList = selectedLocalFileList;
    }
    
    public void initialize(final SharedFileListManager manager, final LibraryManager libraryManager,
        LibraryPanel libraryPanel, final boolean showLibrary){
        
        this.libraryPanel = libraryPanel;
        
        // once this is selected, show all the submenus
        addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                JMenu menu = ShowInListMenu.this;
                menu.removeAll();
                
                if(!selectedFiles.get().isEmpty()) {
                    File selectedFile = selectedFiles.get().get(0);
                    
                    LocalFileList libraryList = libraryManager.getLibraryManagedList();
                    
                    if(showLibrary) {
                        menu.add(new ShowAction(I18n.tr("Library"), getListIcon(libraryList), libraryList, selectedFile));//.setEnabled(selectedLocalFileList == null || libraryManager.getLibraryManagedList() != selectedLocalFileList.get());
                    }
                    manager.getModel().getReadWriteLock().readLock().lock();
                    try {  
                        boolean addSeperator = false;
                        for(SharedFileList fileList : manager.getModel()) {
                        	// only show lists that contain the file and isn't the currently selected list
                            if(fileList.contains(selectedFile) && (selectedLocalFileList == null || fileList != selectedLocalFileList.get())) {
                                menu.add(new ShowAction(fileList.getCollectionName(), getListIcon(fileList), fileList, selectedFile));//.setEnabled(selectedLocalFileList == null || fileList != selectedLocalFileList.get());
                                addSeperator = true;
                            }
                        }
                        if(addSeperator && showLibrary)
                            menu.insertSeparator(1);
                    } finally {
                        manager.getModel().getReadWriteLock().readLock().unlock();
                    }
                }
                if(menu.getMenuComponentCount() < 1)
                    menu.add(new JMenuItem(I18n.tr("empty"))).setEnabled(false);
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
    
    private Icon getListIcon(LocalFileList localFileList) {
        if(localFileList instanceof SharedFileList) {
            if(((SharedFileList)localFileList).isPublic())
                return icons.publicIcon;
            else if(((SharedFileList)localFileList).getFriendIds().size() == 0)
                return icons.unsharedIcon;
            else
                return icons.sharedIcon;
        } else 
            return icons.libraryIcon;
    }
    
    private class ShowAction extends AbstractAction {
        private final LocalFileList localFileList;
        private final File selectedFile;
        
        public ShowAction(String text, Icon icon, LocalFileList localFileList, File selectedFile) {
            super(text);
            putValue(SMALL_ICON, icon);
            this.localFileList = localFileList;
            this.selectedFile = selectedFile;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            libraryPanel.selectLocalFileList(localFileList);
            libraryPanel.selectAndScrollTo(selectedFile);
        }
    }
    
    private static class ShowInListMenuIcons {
        @Resource
        private Icon libraryIcon;
        @Resource
        private Icon publicIcon;
        @Resource
        private Icon unsharedIcon;
        @Resource
        private Icon sharedIcon;
        
        public ShowInListMenuIcons() {
            GuiUtils.assignResources(this);
        }
    }
}
