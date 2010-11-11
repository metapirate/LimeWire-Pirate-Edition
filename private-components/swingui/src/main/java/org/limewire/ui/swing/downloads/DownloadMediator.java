package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inject.LazySingleton;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class DownloadMediator {
    
    public static enum SortOrder {ORDER_ADDED, NAME, PROGRESS, TIME_REMAINING, SPEED, STATUS, FILE_TYPE, EXTENSION};

    /**
     * unfiltered - common to all tables
     */
    private final SortedList<DownloadItem> downloadsCommonBaseList;
	private final DownloadListManager downloadListManager;
	private final Provider<MainDownloadPanel> downloadPanelFactory;
	private final Provider<DownloadHeaderPopupMenu> headerPopupMenuFactory;
	private final Provider<TransferTrayNavigator> transferTrayNavigator;
	
	private EventList<DownloadItem> activeList;
	private JButton clearFinishedButton;
	private JButton fixStalledButton;
	private List<JButton> headerButtons;
	private DownloadHeaderPopupMenu headerPopupMenu;
	
    private final Set<SortOrder> sortInspectionSet = new HashSet<SortOrder>();
    
	@Inject
	public DownloadMediator(DownloadListManager downloadManager,
	        Provider<MainDownloadPanel> downloadPanelFactory,
	        Provider<DownloadHeaderPopupMenu> headerPopupMenuFactory, 
	        Provider<TransferTrayNavigator> transferTrayNavigator) {
	    this.downloadListManager = downloadManager;
	    this.downloadPanelFactory = downloadPanelFactory;
	    this.headerPopupMenuFactory = headerPopupMenuFactory;
	    this.transferTrayNavigator = transferTrayNavigator;
	    
	    EventList<DownloadItem> baseList = GlazedListsFactory.filterList(downloadManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));
	    downloadsCommonBaseList = GlazedListsFactory.sortedList(baseList, getSortComparator(getSortOrder(), isSortAscending()));
	}
    
	/**
	 * Registers listeners to update state.
	 */
    @Inject
    void register() {
        // Add listener to display download table when download added.
        downloadListManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (DownloadListManager.DOWNLOAD_ADDED.equals(evt.getPropertyName())) {
                    transferTrayNavigator.get().selectDownloads();
                }
            }
        });
        
        // Add setting listener to clear finished downloads.  When set, we
        // clear finished downloads and hide the "clear finished" button.
        SharingSettings.CLEAR_DOWNLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        boolean clearDownloads = SharingSettings.CLEAR_DOWNLOAD.getValue();
                        if (clearDownloads) {
                            clearFinished();
                        }
                        if (clearFinishedButton != null) {
                            clearFinishedButton.setVisible(!clearDownloads);
                        }
                    }
                });
            }
        });

        // Add list listeners to enable/show header buttons.
        EventList<DownloadItem> doneList = GlazedListsFactory.filterList(getDownloadList(), 
                new DownloadStateMatcher(DownloadState.DONE,
                        DownloadState.DANGEROUS,
                        DownloadState.THREAT_FOUND,
                        DownloadState.SCAN_FAILED));
        EventList<DownloadItem> stalledList = GlazedListsFactory.filterList(getDownloadList(), 
                new DownloadStateMatcher(DownloadState.STALLED));

        doneList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                if (clearFinishedButton != null) {
                    clearFinishedButton.setEnabled(listChanges.getSourceList().size() > 0);
                }
            }
        });
        
        stalledList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                if (fixStalledButton != null) {
                    fixStalledButton.setVisible(listChanges.getSourceList().size() != 0);
                }
            }
        });
    }
    
	public JComponent getComponent() {
	    return downloadPanelFactory.get();
	}
	
	public boolean isSortAscending() {
	    return SwingUiSettings.DOWNLOAD_SORT_ASCENDING.getValue();
	}
	
	public SortOrder getSortOrder() {
	    try {
	        String sortKey = SwingUiSettings.DOWNLOAD_SORT_KEY.get();
	        return SortOrder.valueOf(sortKey);
	    } catch (IllegalArgumentException ex) {
            // Return default order if setting is invalid.
	        return SortOrder.ORDER_ADDED;
	    }
	}
	
	public void setSortOrder(SortOrder order, boolean isAscending){
        // Save sort settings.
	    SwingUiSettings.DOWNLOAD_SORT_KEY.set(order.toString());
	    SwingUiSettings.DOWNLOAD_SORT_ASCENDING.setValue(isAscending);
	    
        // Apply sort order.
	    downloadsCommonBaseList.setComparator(getSortComparator(order, isAscending));
	    
	    sortInspectionSet.add(order);
	}
	
    /**
     * Returns a comparator for the specified sort key and direction.
     */
	private Comparator<DownloadItem> getSortComparator(SortOrder sortOrder, boolean ascending) {
        Comparator<DownloadItem> comparator;
        switch (sortOrder) {
        case ORDER_ADDED:
            comparator = new OrderAddedComparator();
            break;
        case NAME:
            comparator = new NameComparator();
            break;
        case PROGRESS:
            comparator = new ProgressComparator();
            break;
        case TIME_REMAINING:
            comparator = new TimeRemainingComparator();
            break;
        case SPEED:
            comparator = new SpeedComparator();
            break;
        case STATUS:
            comparator = new DownloadStateComparator();
            break;
        case FILE_TYPE:
            comparator = new FileTypeComparator();
            break;
        case EXTENSION:
            comparator = new FileExtensionComparator();
            break;
        default:
            throw new IllegalArgumentException("Unknown SortOrder: " + sortOrder);
        }
        
        if (ascending) {
            return comparator;
        } else {
            return new DescendingComparator(comparator);
        }
	}

	public void pauseAll() {
        downloadsCommonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : downloadsCommonBaseList) {
                if (item.getState().isPausable()) {
                    item.pause();
                }
            }
        } finally {
            downloadsCommonBaseList.getReadWriteLock().writeLock().unlock();
        }
    }

	public void resumeAll() {
        downloadsCommonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : downloadsCommonBaseList) {
                if (item.getState().isResumable()) {
                    item.resume();
                }
            }
        } finally {
            downloadsCommonBaseList.getReadWriteLock().writeLock().unlock();
        }
    }
	
    /**
     * Returns a list of active download items.
     */
	public EventList<DownloadItem> getActiveList() {
	    if (activeList == null) {
	        activeList = GlazedListsFactory.filterList(downloadsCommonBaseList, 
	                new DownloadStateExcluder(DownloadState.ERROR,
	                        DownloadState.DONE,
	                        DownloadState.CANCELLED,
	                        DownloadState.DANGEROUS,
	                        DownloadState.THREAT_FOUND,
	                        DownloadState.SCAN_FAILED));
	    }
	    return activeList;
	}
	
    /**
     * Returns a sorted list of downloads.
     */
	public EventList<DownloadItem> getDownloadList() {
	    return downloadsCommonBaseList;
	}
	
    /**
     * Returns a list of header buttons.
     */
	public List<JButton> getHeaderButtons() {
	    if (headerButtons == null) {
	        // Create buttons.
            fixStalledButton = new HyperlinkButton(new FixStalledAction());
            fixStalledButton.setVisible(false);
	        
            clearFinishedButton = new HyperlinkButton(new ClearFinishedAction());
            clearFinishedButton.setEnabled(false);
	        
            // Add buttons to list.
            headerButtons = new ArrayList<JButton>();
            headerButtons.add(fixStalledButton);
            headerButtons.add(clearFinishedButton);
	    }
	    return headerButtons;
	}
	
    /**
     * Returns the header popup menu associated with the downloads table.
     */
	public JPopupMenu getHeaderPopupMenu() {
	    if (headerPopupMenu == null) {
	        headerPopupMenu = headerPopupMenuFactory.get();
	        headerPopupMenu.addPopupMenuListener(new PopupMenuListener() {
	            @Override
	            public void popupMenuCanceled(PopupMenuEvent e) {
	                headerPopupMenu.removeAll();
	            }
	            @Override
	            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
	                headerPopupMenu.removeAll();
	            }
	            @Override
	            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	                headerPopupMenu.populate();
	            }
	        });
	    }
	    return headerPopupMenu;
	}
	
	public void clearFinished() {
	    downloadListManager.clearFinished();
	}

    public void fixStalled() {
        List<DownloadItem> items = getMatchingDownloadItems(DownloadState.STALLED);
        for (DownloadItem item : items) {
            item.resume();
        }
    }

    public void cancelStalled() {
        cancelMatchingDownloadItems(DownloadState.STALLED);
    }

    public void cancelError() {
        cancelMatchingDownloadItems(DownloadState.ERROR);
    }

    public void cancelAll() {
        cancelMatchingDownloadItems(null);
    }
    
    public boolean hasResumable() {
        downloadsCommonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : downloadsCommonBaseList) {
                if(item.getState().isResumable())
                    return true;
            }
        } finally {
            downloadsCommonBaseList.getReadWriteLock().writeLock().unlock();
        }
        return false;
    }
    
    public boolean hasPausable() {
        downloadsCommonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : downloadsCommonBaseList) {
                if(item.getState().isPausable())
                    return true;
            }
        } finally {
            downloadsCommonBaseList.getReadWriteLock().writeLock().unlock();
        }
        return false;
    }
    
    public boolean containsState(DownloadState state) {
        return getMatchingDownloadItems(state).size() > 0;
    }
    
    /**
     * 
     * @param state The state of the DownloadItems to be canceled.  Null will cancel all.
     */
    private void cancelMatchingDownloadItems(DownloadState state){
        List<DownloadItem> items = getMatchingDownloadItems(state);
        for(DownloadItem item : items){
            item.cancel();
        }
    }
    
    /**
     * 
     * @param state null will return all DownloadItems
     * @return a List of all DownloadItems in the specified DownloadState
     */
    private List<DownloadItem> getMatchingDownloadItems(DownloadState state) {
        if (state == null) {
            return new ArrayList<DownloadItem>(downloadsCommonBaseList);
        }
        
        List<DownloadItem> matchingItems = new ArrayList<DownloadItem>();
        for (DownloadItem item : downloadsCommonBaseList) {
            if (item.getState() == state) {
                matchingItems.add(item);
            }
        }
        
        return matchingItems;
    }
    
    /**
     * Action to clear all finished downloads.
     */
    private class ClearFinishedAction extends AbstractAction {

        public ClearFinishedAction() {
            super(I18n.tr("Clear Finished"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            clearFinished();
        }
    }
    
    /**
     * Action to fix all stalled downloads.
     */
    private class FixStalledAction extends AbstractAction {

        public FixStalledAction() {
            super(I18n.tr("Fix Stalled"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            fixStalled();
        }
    }  
    
    private static class OrderAddedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) { 
            if (o1 == o2){
                return 0;
            }
            return o2.getStartDate().compareTo(o1.getStartDate());
        }      
    }
    
    private static class NameComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getTitle().compareTo(o2.getTitle());
        }   
     
    } 
  
    private static class ProgressComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getPercentComplete() - o2.getPercentComplete();
        }   
     
    } 
    
    private static class TimeRemainingComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)(o1.getRemainingDownloadTime() - o2.getRemainingDownloadTime());
        }   
     
    }
    
    
    private static class SpeedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)o2.getDownloadSpeed() - (int)o1.getDownloadSpeed();
        }   
     
    }
    
    
    private static class FileTypeComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getCategory().compareTo(o2.getCategory());
        }   
     
    } 

    
    private static class FileExtensionComparator implements Comparator<DownloadItem> {
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return FileUtils.getFileExtension(o1.getDownloadingFile()).compareTo(FileUtils.getFileExtension(o2.getDownloadingFile()));
        }   
     
    }
    
    private static class DescendingComparator implements Comparator<DownloadItem>{
        private Comparator<DownloadItem> delegate;

        public DescendingComparator(Comparator<DownloadItem> delegate){
            this.delegate = delegate;
        }

        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            return -1 * delegate.compare(o1, o2);
        }
    }

    public void selectAndScrollTo(URN urn) {
        transferTrayNavigator.get().selectDownloads();
        downloadPanelFactory.get().selectAndScrollTo(urn);
    }
    
}
