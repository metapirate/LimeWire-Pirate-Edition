package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.library.table.ListMenuFactory;
import org.limewire.ui.swing.search.BlockUserMenuFactory;
import org.limewire.ui.swing.search.RemoteHostMenuFactory;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Popup menu for the Uploads table.
 */
public class UploadPopupMenu extends JPopupMenu {

    private final UploadTable table;
    private final List<UploadItem> uploadItems;
    private final Provider<List<File>> selectedFiles;
    private final UploadActionHandler actionHandler;
    private final LibraryManager libraryManager;
    private final RemoteHostMenuFactory browseMenuFactory;
    private final BlockUserMenuFactory blockUserMenuFactory;
    private final ListMenuFactory listMenuFactory;
    
    private Collection<RemoteHost> remoteHosts;
    
    @Inject
    public UploadPopupMenu(
            @Assisted UploadTable table,
            @Assisted List<UploadItem> uploadItems,
            @FinishedUploadSelected Provider<List<File>> selectedFiles,
            UploadActionHandler actionHandler,
            LibraryManager libraryManager,
            RemoteHostMenuFactory browseMenuFactory,
            BlockUserMenuFactory blockUserMenuFactory,
            ListMenuFactory listMenuFactory) {
        this.table = table;
        this.uploadItems = uploadItems;
        this.selectedFiles = selectedFiles;
        this.actionHandler = actionHandler;
        this.libraryManager = libraryManager;
        this.browseMenuFactory = browseMenuFactory;
        this.blockUserMenuFactory = blockUserMenuFactory;
        this.listMenuFactory = listMenuFactory;
        
        createMenu();
    }
    
    /**
     * Builds the menu.
     */
    private void createMenu() {
        int itemCount = uploadItems.size();
        if (itemCount == 0) {
            throw new IllegalStateException(I18n.tr("No selected items"));
            
        } else if (itemCount == 1) {
            UploadItem item = uploadItems.get(0);
            switch (item.getUploadItemType()) {
            case GNUTELLA:
                createSingleGnutellaMenu(item);
                break;
                
            case BITTORRENT:
                createSingleTorrentMenu(item);
                break;
                
            default:
                throw new IllegalStateException(I18n.tr("Unknown upload type: " + item.getUploadItemType()));
            }
            
        } else {
            // Multiple items selected.
            createMultipleItemMenu();
        }
        
    }
    
    /**
     * Builds the menu for a single Gnutella upload item.
     */
    private void createSingleGnutellaMenu(UploadItem uploadItem) {
        ActionListener listener = new DefaultMenuListener();
        
        boolean done = UploadMediator.isRemovable(uploadItem);
        boolean browseItem = UploadMediator.isBrowseHost(uploadItem);
        
        if (done) {
            add(createRemoveMenuItem());
        } else {
            add(createCancelMenuItem(listener));
        }
        
        if (!browseItem) {
            if (getComponentCount() > 0) {
                addSeparator();
            }

            JMenuItem launchMenuItem = new JMenuItem(I18n.tr("Play/Open"));
            launchMenuItem.setActionCommand(isPlayable(uploadItem.getCategory()) ?
                    UploadActionHandler.PLAY_COMMAND : UploadActionHandler.LAUNCH_COMMAND);
            launchMenuItem.addActionListener(listener);
            add(launchMenuItem);

            JMenuItem locateOnDiskMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
            locateOnDiskMenuItem.setActionCommand(UploadActionHandler.LOCATE_ON_DISK_COMMAND);
            locateOnDiskMenuItem.addActionListener(listener);
            add(locateOnDiskMenuItem);

            JMenuItem showInLibraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
            showInLibraryMenuItem.setActionCommand(UploadActionHandler.LIBRARY_COMMAND);
            showInLibraryMenuItem.addActionListener(listener);
            add(showInLibraryMenuItem).setEnabled(libraryManager.getLibraryManagedList().contains(uploadItem.getUrn()));
        }
        
        if (done && !browseItem) {
            addSeparator();

            JMenu addToListMenu = listMenuFactory.createAddToListMenu(selectedFiles);
            add(addToListMenu);

            JMenu showInListMenu = listMenuFactory.createShowInListMenu(selectedFiles, true);
            add(showInListMenu);
        }
        
        addSeparator();

        JMenu browseMenu = browseMenuFactory.createBrowseMenu(getRemoteHosts());
        add(browseMenu);
        
        JMenu blockMenu = blockUserMenuFactory.createDownloadBlockMenu(getRemoteHosts());
        if (blockMenu != null) {
            add(blockMenu);
        }
        
        if (!browseItem) {
            addSeparator();
            
            add(createFileInfoMenuItem(listener));
        }
    }
    
    /**
     * Builds the menu for a single BitTorrent upload item.
     */
    private void createSingleTorrentMenu(UploadItem uploadItem) {
        ActionListener listener = new DefaultMenuListener();

        boolean done = UploadMediator.isRemovable(uploadItem);
        boolean browseItem = UploadMediator.isBrowseHost(uploadItem);
        boolean pausable = UploadMediator.isPausable(uploadItem);
        boolean resumable = UploadMediator.isResumable(uploadItem);
        
        if (pausable) {
            add(createPauseMenuItem());
        }
        if (resumable) {
            add(createResumeMenuItem());
        }
        
        if (done) {
            add(createRemoveMenuItem());
        } else {
            add(createCancelMenuItem(listener));
        }
        
        if (!browseItem) {
            if (getComponentCount() > 0) {
                addSeparator();
            }

            JMenuItem locateOnDiskMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
            locateOnDiskMenuItem.setActionCommand(UploadActionHandler.LOCATE_ON_DISK_COMMAND);
            locateOnDiskMenuItem.addActionListener(listener);
            add(locateOnDiskMenuItem);
        }
        
        if (done) {
            if (getComponentCount() > 0) {
                addSeparator();
            }

            JMenu addToListMenu = listMenuFactory.createAddToListMenu(selectedFiles);
            add(addToListMenu);
        }
            
        if (!browseItem) {
            if (getComponentCount() > 0) {
                addSeparator();
            }
            
            add(createFileInfoMenuItem(listener));
        }
    }
    
    /**
     * Builds the menu for multiple upload items.
     */
    private void createMultipleItemMenu() {
        boolean allBrowse = isAllBrowse();
        boolean allDone = isAllDone();
        boolean anyPausable = isAnyPausable();
        boolean anyResumable = isAnyResumable();
        
        if (!allDone) {
            if (anyPausable) {
                add(createPauseMenuItem());
            }
            if (anyResumable) {
                add(createResumeMenuItem());
            }
        }
        
        if (allDone || isAnyRemovable()) {
            add(createRemoveMenuItem());
        }
        
        if (!allDone) {
            if (getComponentCount() > 0) {
                addSeparator();
            }

            JMenu browseMenu = browseMenuFactory.createBrowseMenu(getRemoteHosts());
            add(browseMenu);

            JMenu blockMenu = blockUserMenuFactory.createDownloadBlockMenu(getRemoteHosts());
            if (blockMenu != null) {
                add(blockMenu);
            }

            addSeparator();
            
            add(createCancelMenuItem(new DefaultMenuListener()));
            
        } else if (!allBrowse) {
            if (getComponentCount() > 0) {
                addSeparator();
            }
            
            JMenu addToListMenu = listMenuFactory.createAddToListMenu(selectedFiles);
            add(addToListMenu);
        }
    }
    
    /**
     * Creates a Cancel menu item with the specified action listener.
     */
    private JMenuItem createCancelMenuItem(ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(I18n.tr("Cancel"));
        menuItem.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
        menuItem.addActionListener(listener);
        return menuItem;
    }
    
    /**
     * Creates a Remove menu item.
     */
    private JMenuItem createRemoveMenuItem() {
        JMenuItem menuItem = new JMenuItem(I18n.tr("Clear from Tray"));
        menuItem.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        menuItem.addActionListener(new RemoveMenuListener());
        return menuItem;
    }
    
    /**
     * Creates a Pause menu item.
     */
    private JMenuItem createPauseMenuItem() {
        JMenuItem menuItem = new JMenuItem(I18n.tr("Pause"));
        menuItem.setActionCommand(UploadActionHandler.PAUSE_COMMAND);
        menuItem.addActionListener(new PauseMenuListener());
        return menuItem;
    }
    
    /**
     * Creates a Resume menu item.
     */
    private JMenuItem createResumeMenuItem() {
        JMenuItem menuItem = new JMenuItem(I18n.tr("Resume"));
        menuItem.setActionCommand(UploadActionHandler.RESUME_COMMAND);
        menuItem.addActionListener(new ResumeMenuListener());
        return menuItem;
    }
    
    /**
     * Creates a View Info menu item.
     */
    private JMenuItem createFileInfoMenuItem(ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(I18n.tr("View File Info..."));
        menuItem.setActionCommand(UploadActionHandler.PROPERTIES_COMMAND);
        menuItem.addActionListener(listener);
        return menuItem;
    }
    
    /**
     * Returns the remote hosts associated with the upload items.  Torrent
     * items are excluded.
     */
    private Collection<RemoteHost> getRemoteHosts() {
        if (remoteHosts == null) {
            remoteHosts = new ArrayList<RemoteHost>();
            for (UploadItem item : uploadItems) {
                if (item.getUploadItemType() != UploadItemType.BITTORRENT) {
                    remoteHosts.add(item.getRemoteHost());
                }
            }
        }
        return remoteHosts;
    }
    
    /**
     * Returns true if all upload items are browse items.
     */
    private boolean isAllBrowse() {
        for (UploadItem item : uploadItems) {
            if (!UploadMediator.isBrowseHost(item)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns true if all upload items are done.
     */
    private boolean isAllDone() {
        for (UploadItem item : uploadItems) {
            if (!UploadMediator.isRemovable(item)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns true if any upload items are pausable.
     */
    private boolean isAnyPausable() {
        for (UploadItem item : uploadItems) {
            if (UploadMediator.isPausable(item)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if any upload items are resumable.
     */
    private boolean isAnyResumable() {
        for (UploadItem item : uploadItems) {
            if (UploadMediator.isResumable(item)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if any upload items are removable.
     */
    private boolean isAnyRemovable() {
        for (UploadItem item : uploadItems) {
            if (UploadMediator.isRemovable(item)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if the specified Category represents a playable file.
     */
    private boolean isPlayable(Category category) {
        return (category == Category.AUDIO) || (category == Category.VIDEO);
    }
    
    /**
     * Cancels cell editing on the table.
     */
    private void cancelEditing() {
        Component comp = table.getEditorComponent();
        if (comp instanceof TableCellEditor) {
            ((TableCellEditor)comp).cancelCellEditing();
        }
    }
    
    /**
     * Default menu listener that accepts all upload items.
     */
    private class DefaultMenuListener extends MenuListener {
        @Override
        protected boolean isValid(UploadItem item) {
            return true;
        }
    }
    
    /**
     * Menu listener to pause upload items.
     */
    private class PauseMenuListener extends MenuListener {
        @Override
        protected boolean isValid(UploadItem item) {
            return UploadMediator.isPausable(item);
        }
    }
    
    /**
     * Menu listener to resume upload items.
     */
    private class ResumeMenuListener extends MenuListener {
        @Override
        protected boolean isValid(UploadItem item) {
            return UploadMediator.isResumable(item);
        }
    }
    
    /**
     * Menu listener to remove upload items.
     */
    private class RemoveMenuListener extends MenuListener {
        @Override
        protected boolean isValid(UploadItem item) {
            return UploadMediator.isRemovable(item);
        }
    }
    
    /**
     * Base class for menu item action listeners.
     */
    private abstract class MenuListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            for (UploadItem item : uploadItems) {
                if (isValid(item)) {
                    actionHandler.performAction(e.getActionCommand(), item);
                }
            }
            // must cancel editing
            cancelEditing();
        }
        
        /**
         * Returns true if the specified upload item can be acted on.
         */
        protected abstract boolean isValid(UploadItem item);
    }
}
