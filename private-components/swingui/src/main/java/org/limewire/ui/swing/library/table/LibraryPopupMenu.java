package org.limewire.ui.swing.library.table;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.LibrarySelected;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryPopupMenu extends JPopupMenu {

    private final Provider<List<File>> selectedFiles;
    private final Provider<LocalFileList> selectedLocalFileList;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final ListMenuFactory listMenuFactory;
    private final Provider<LaunchFileAction> launchAction;
    private final Provider<RenameFileAction> renameFileAction;
    private final Provider<LocateFileAction> locateAction;
    private final Provider<RemoveFromListMenu> removeFromListMenu;
    private final Provider<RemoveFromListAction> removeFromListAction;
    private final DeleteAction deleteAction;
    private final Provider<ViewFileInfoAction> fileInfoAction;
    
    @Inject
    public LibraryPopupMenu(
            @LibrarySelected Provider<List<File>> selectedFiles,
            @LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            ListMenuFactory listMenuFactory, Provider<RemoveFromListMenu> removeFromListMenu,
            Provider<RemoveFromListAction> removeFromListAction,
            Provider<LaunchFileAction> launchAction, Provider<LocateFileAction> locateAction, 
            Provider<RenameFileAction> renameFileAction,
            DeleteAction deleteAction, Provider<ViewFileInfoAction> fileInfoAction) {
        this.selectedFiles = selectedFiles;
        this.selectedLocalFileList = selectedLocalFileList;
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.listMenuFactory = listMenuFactory;
        this.launchAction = launchAction;
        this.renameFileAction = renameFileAction;
        this.locateAction = locateAction;
        this.removeFromListMenu = removeFromListMenu;
        this.removeFromListAction = removeFromListAction;
        this.deleteAction = deleteAction;
        this.fileInfoAction = fileInfoAction;
        
        init();
    }
    
    private void init() {
        if(selectedLocalFileList.get() instanceof LibraryFileList) {
            initLibrary();
        } else {
            initPlayList();
        }
    }
    
    private void initLibrary() {
        List<LocalFileItem> localFileItem = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        // if single selection
        if(localFileItem.size() == 1) {
            add(launchAction.get());
            addSeparator();
            
            // add to list, show in list, remove from list
            add(listMenuFactory.createAddToListMenu(selectedFiles));
            add(listMenuFactory.createShowInListMenu(selectedFiles, selectedLocalFileList, false));
            add(removeFromListMenu.get());
            addSeparator();
            
            add(renameFileAction.get()).setEnabled(!localFileItem.get(0).isIncomplete());
            add(locateAction.get());
            add(deleteAction);
            addSeparator();
            add(fileInfoAction.get());
        } else {
            add(listMenuFactory.createAddToListMenu(selectedFiles));
            add(removeFromListMenu.get());
            addSeparator();
            add(deleteAction);
        }
    }
    
    private void initPlayList() {
        List<LocalFileItem> localFileItem = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        // if single selection
        if(localFileItem.size() == 1) {
            add(launchAction.get());
            addSeparator();
            
            // add to list, show in list, remove from list
            add(listMenuFactory.createShowInListMenu(selectedFiles, selectedLocalFileList, true));
            add(removeFromListAction.get());
            addSeparator();
            
            add(renameFileAction.get()).setEnabled(!localFileItem.get(0).isIncomplete());
            add(locateAction.get());
            add(deleteAction);
            addSeparator();
            add(fileInfoAction.get());
        } else {
            add(removeFromListAction.get());
            addSeparator();
            add(deleteAction);
        }
    }
}
