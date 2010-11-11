package org.limewire.ui.swing.downloads.table;

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
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.library.table.ListMenuFactory;
import org.limewire.ui.swing.search.BlockUserMenuFactory;
import org.limewire.ui.swing.search.RemoteHostMenuFactory;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

public class DownloadTableMenu extends JPopupMenu{
    
    private final MenuListener menuListener;
    private final DownloadActionHandler actionHandler;
    private final DownloadTable table;
    private final RemoteHostMenuFactory remoteHostMenuFactory;
    private final BlockUserMenuFactory blockUserMenuFactory;
    private final ListMenuFactory listMenuFactory;
    private final Provider<List<File>> selectedFiles;
    
    private List<DownloadItem> downloadItems;

    /**
     * Constructs a DownloadTableMenu using the specified action handler and
     * display table.
     */
    @Inject
    public DownloadTableMenu(RemoteHostMenuFactory remoteHostMenuFactory,
            BlockUserMenuFactory blockUserMenuFactory, 
            DownloadActionHandler actionHandler,
            ListMenuFactory listMenuFactory,
            @FinishedDownloadSelected Provider<List<File>> selectedFiles,
            @Assisted DownloadTable table) {
        this.remoteHostMenuFactory = remoteHostMenuFactory;
        this.blockUserMenuFactory = blockUserMenuFactory;
        this.actionHandler = actionHandler;
        this.table = table;
        this.listMenuFactory = listMenuFactory;
        this.selectedFiles = selectedFiles;

        menuListener = new MenuListener();   
        
        update(table.getSelectedItems());
    }

  
    public void update(List<DownloadItem> downloadItems) {
        this.downloadItems = downloadItems;
        removeAll();
        if (downloadItems.size() == 1) {
            initializeSingleItemMenu(downloadItems.get(0));
        } else {
            initializeMultiItemMenu(downloadItems);
        }
    }
    

    private void initializeSingleItemMenu(DownloadItem downloadItem){
        
        DownloadState state = downloadItem.getState();
        
        if (state == DownloadState.DONE ||
                state == DownloadState.SCAN_FAILED){
            add(createLaunchMenuItem()).setEnabled(downloadItem.isLaunchable());
            add(createRemoveMenuItem());
            addSeparator();
            add(createLocateOnDiskMenuItem());
            add(createLocateInLibraryMenuItem());
            addSeparator();
            add(listMenuFactory.createAddToListMenu(selectedFiles));
            add(listMenuFactory.createShowInListMenu(selectedFiles, true));
            addSeparator();
            add(createPropertiesMenuItem());
            
        } else if (state == DownloadState.DANGEROUS || 
                state == DownloadState.THREAT_FOUND) {
            add(createRemoveMenuItem());
            
        } else {
            //not DONE
            if(isResumable(state)){
                add(createResumeMenuItem());
            }
            if(isPausable(state)){
                add(createPauseMenuItem());
            }
            if(isTryAgainable(state)){
                add(createTryAgainMenuItem());
            }            
            if (downloadItem.getCategory() != Category.PROGRAM
                    && downloadItem.getCategory() != Category.OTHER) {
                add(createPreviewMenuItem()).setEnabled(downloadItem.isLaunchable());
            }
            addSeparator();
            
            add(createLocateOnDiskMenuItem());
            add(createLocateInLibraryMenuItem());
            if (downloadItem.isRelocatable()) {
                add(createChangeLocactionMenuItem());
            }
            addSeparator();
            boolean hasBrowse = maybeAddBrowseMenu(downloadItem.getRemoteHosts());
            boolean hasBlock = maybeAddBlockMenu(downloadItem.getRemoteHosts());
            if (hasBrowse || hasBlock) {
                addSeparator();
            }
            if(state == DownloadState.ERROR){
                add(createCancelWithRemoveNameMenuItem());
            } else {
                add(createCancelMenuItem());
            }
            addSeparator();
            add(createPropertiesMenuItem()); 
        }        
    }  
    
    private void initializeMultiItemMenu(List<DownloadItem> downloadItems) {
        boolean hasTryAgain = false;
        boolean hasPause = false;
        boolean hasCancel = false;
        boolean hasResume = false;
        boolean allDone = true;

        //hosts to browse or block
        List<RemoteHost> hosts = new ArrayList<RemoteHost>();
        
        //Check which menu items to include.  Items are included if they are valid
        //for any item in the list.
        for(DownloadItem item : downloadItems){
            if (hasTryAgain && hasPause && hasCancel && hasResume){
                //if all four booleans are true, we are done checking
                break;
            }
            if(item.getState() != DownloadState.DONE &&
                    item.getState() != DownloadState.SCAN_FAILED){
                allDone = false;
            }
            if(isResumable(item.getState())){
                hasResume = true;
            }
            if(isPausable(item.getState())){
                hasPause = true;
            }
            if(isTryAgainable(item.getState())){
                hasTryAgain = true;
            }
            if(isCancelable(item.getState())){
                hasCancel = true;
            }
            hosts.addAll(item.getRemoteHosts());
        }
        
        if(allDone){
            add(createCancelWithRemoveNameMenuItem());
            add(listMenuFactory.createAddToListMenu(selectedFiles));
        } else {
            if (hasPause) {
                add(createPauseMenuItem());
            }
            if (hasResume) {
                add(createResumeMenuItem());
            }
            if (hasTryAgain) {
                add(createTryAgainMenuItem());
            }

            maybeAddBrowseMenu(hosts);
            maybeAddBlockMenu(hosts);

            if (hasCancel) {
                add(createCancelMenuItem());
            }
        }
        
    }

    private boolean isPausable(DownloadState state) {
        return state.isPausable();
    }
    
    private boolean isResumable(DownloadState state) {
        return state == DownloadState.PAUSED;
    }

    private boolean isTryAgainable(DownloadState state) {
        return state == DownloadState.STALLED;
    }

    private boolean isCancelable(DownloadState state) {
        return !state.isFinished();
    }
    
    private boolean maybeAddBrowseMenu(Collection<RemoteHost> remoteHosts){
        if (remoteHosts.size() > 0) {
            add(remoteHostMenuFactory.createBrowseMenu(remoteHosts));
            return true;
        }
        return false;
    }
    
    private boolean maybeAddBlockMenu(Collection<RemoteHost> remoteHosts){
        JMenu blockMenu = blockUserMenuFactory.createDownloadBlockMenu(remoteHosts);
        if(blockMenu!= null){
            add(blockMenu);
            return true;
        }
        return false;
    }
    
    private void cancelEditing(){
        Component comp = table.getEditorComponent();
        if (comp != null && comp instanceof TableCellEditor) {
            ((TableCellEditor) comp).cancelCellEditing();
        }
    }
    
    private JMenuItem createPauseMenuItem(){
        JMenuItem pauseMenuItem = new JMenuItem(I18n.tr("Pause"));
        pauseMenuItem.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
        pauseMenuItem.addActionListener(new PauseListener());
        return pauseMenuItem;
    }   
    
    private JMenuItem createCancelMenuItem(){
        JMenuItem cancelMenuItem = new JMenuItem(I18n.tr("Cancel Download"));
        cancelMenuItem.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelMenuItem.addActionListener(new CancelListener());
        return cancelMenuItem;
    }  
    
    private JMenuItem createResumeMenuItem(){
        JMenuItem resumeMenuItem = new JMenuItem(I18n.tr("Resume"));
        resumeMenuItem.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
        resumeMenuItem.addActionListener(new ResumeListener());
        return resumeMenuItem;
    }   
    
    private JMenuItem createTryAgainMenuItem(){
        JMenuItem tryAgainMenuItem = new JMenuItem(I18n.tr("Try Again"));
        tryAgainMenuItem.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        tryAgainMenuItem.addActionListener(new TryAgainListener());
        return tryAgainMenuItem;
    }   
    
    private JMenuItem createLaunchMenuItem(){
        JMenuItem launchMenuItem = new JMenuItem(I18n.tr("Play/Open"));
        launchMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);
        return launchMenuItem;
    }  
    
    private JMenuItem createRemoveMenuItem(){
        JMenuItem removeMenuItem = new JMenuItem(I18n.tr("Clear from Tray"));
        removeMenuItem.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);
        return removeMenuItem;
    }   
    
    private JMenuItem createCancelWithRemoveNameMenuItem(){
        JMenuItem cancelWithRemoveNameMenuItem = new JMenuItem(I18n.tr("Clear from Tray"));
        cancelWithRemoveNameMenuItem.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelWithRemoveNameMenuItem.addActionListener(menuListener);
        return cancelWithRemoveNameMenuItem;
    } 
    
    private JMenuItem createLocateOnDiskMenuItem(){
        JMenuItem locateMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
        locateMenuItem.setActionCommand(DownloadActionHandler.LOCATE_COMMAND);
        locateMenuItem.addActionListener(menuListener);
        return locateMenuItem;
    } 
    
    private JMenuItem createLocateInLibraryMenuItem(){
        JMenuItem libraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
        libraryMenuItem.setActionCommand(DownloadActionHandler.LIBRARY_COMMAND);
        libraryMenuItem.addActionListener(menuListener);
        return libraryMenuItem;
    }   
    
    private JMenuItem createPropertiesMenuItem(){
        JMenuItem propertiesMenuItem = new JMenuItem(I18n.tr("View File Info..."));
        propertiesMenuItem.setActionCommand(DownloadActionHandler.PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);
        return propertiesMenuItem;
    }  
    
    private JMenuItem createChangeLocactionMenuItem(){
        JMenuItem changeLocationMenuItem = new JMenuItem(I18n.tr("Change Location..."));
        changeLocationMenuItem.setActionCommand(DownloadActionHandler.CHANGE_LOCATION_COMMAND);
        changeLocationMenuItem.addActionListener(menuListener);
        return changeLocationMenuItem;
    }
    
    
    //These will be reintroduced later
//    private JMenuItem createRaisePriorityMenuItem(){
//        JMenuItem raisePriorityItem = new JMenuItem(I18n.tr("Raise Priority"));
//        raisePriorityItem.addActionListener(menuListener);       
//
//        return raisePriorityItem;
//    }  
//    
//    private JMenuItem createLowerPriorityMenuItem() {
//        JMenuItem lowerPriorityItem = new JMenuItem(I18n.tr("Lower Priority"));
//        lowerPriorityItem.addActionListener(menuListener);
//        return lowerPriorityItem;
//    }

    private JMenuItem createPreviewMenuItem() {
        JMenuItem previewMenuItem = new JMenuItem(I18n.tr("Preview File"));
        previewMenuItem.setActionCommand(DownloadActionHandler.PREVIEW_COMMAND);
        previewMenuItem.addActionListener(menuListener);
        return previewMenuItem;
    }
   
    /**
     * An ActionListener for the menu items in the popup menu.  
     */
    private class MenuListener implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get download item and perform action.
            for (DownloadItem item : downloadItems) {
                actionHandler.performAction(e.getActionCommand(), item);
            }
            
            // must cancel editing
            cancelEditing();
        }
    }
    
    /**
     * An ActionListener for pausing downloads.  
     */
    private class PauseListener extends PopupActionListener {
        public PauseListener() {
            super(DownloadActionHandler.PAUSE_COMMAND);
        }

        @Override
        boolean isTargetState(DownloadState state) {
            return isPausable(state);
        }
    } 
    
    /**
     * An ActionListener for cancelling downloads. 
     */
    private class CancelListener extends PopupActionListener {
        public CancelListener() {
            super(DownloadActionHandler.CANCEL_COMMAND);
        }

        @Override
        boolean isTargetState(DownloadState state) {
            return isCancelable(state);
        }
    } 
    
    /**
     * An ActionListener for stalled downloads. 
     */
    private class TryAgainListener extends PopupActionListener {
        public TryAgainListener() {
            super(DownloadActionHandler.TRY_AGAIN_COMMAND);
        }

        @Override
        boolean isTargetState(DownloadState state) {
            return isTryAgainable(state);
        }
    } 
    
    /**
     * An ActionListener for resuming downloads. 
     */
    private class ResumeListener extends PopupActionListener {
        public ResumeListener() {
            super(DownloadActionHandler.RESUME_COMMAND);
        }

        @Override
        boolean isTargetState(DownloadState state) {
            return isResumable(state);
        }
    } 
    
    /**
     * An ActionListener for the menu items in the popup menu.  
     */
    private abstract class PopupActionListener implements ActionListener {
        private String command;

        public PopupActionListener(String command){
            this.command = command;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get download item and perform action.
            for (DownloadItem item : downloadItems) {
                if (isTargetState(item.getState())) {
                    actionHandler.performAction(command, item);
                }
            }
            
            // must cancel editing
            cancelEditing();
        }
        
        abstract boolean isTargetState(DownloadState state);
    }    
}
