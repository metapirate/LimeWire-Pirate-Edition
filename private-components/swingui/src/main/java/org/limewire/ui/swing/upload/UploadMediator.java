package org.limewire.ui.swing.upload;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.upload.table.UploadTable;
import org.limewire.ui.swing.upload.table.UploadTableFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Mediator to control the interaction between the uploads table and various
 * services.
 */
@EagerSingleton
public class UploadMediator {
    public enum SortOrder {
        ORDER_STARTED, NAME, PROGRESS, TIME_REMAINING, SPEED, STATUS, 
        FILE_TYPE, FILE_EXTENSION, USER_NAME
    }
    
    public static final String NAME = "UploadPanel";
    
    private final UploadListManager uploadListManager;
    private final Provider<TorrentManager> torrentManager;
    private final UploadTableFactory uploadTableFactory;
    private final Provider<TransferTrayNavigator> transferTrayNavigator;
    
    private EventList<UploadItem> activeList;
    private SortedList<UploadItem> sortedList;
    
    private JPanel uploadPanel;
    private UploadTable uploadTable;
    
    private JButton clearFinishedButton;
    private List<JButton> headerButtons;
    private JPopupMenu headerPopupMenu;
    
    
    @Inject
    public UploadMediator(UploadListManager uploadListManager, Provider<TorrentManager> torrentManager,            
            UploadTableFactory uploadTableFactory, Provider<TransferTrayNavigator> transferTrayNavigator) {
        this.uploadListManager = uploadListManager;
        this.torrentManager = torrentManager;
        this.uploadTableFactory = uploadTableFactory;
        this.transferTrayNavigator = transferTrayNavigator;
        
        sortedList = GlazedListsFactory.sortedList(uploadListManager.getSwingThreadSafeUploads(),
                new OrderedComparator<UploadItem>(getSortComparator(getSortOrder()), isSortAscending()));
    }
    
    /**
     * Start the (polling) upload monitor.  
     * <p>
     * Note: this only makes sense if this component is created on demand.
     */
    @Inject
    public void register(ServiceRegistry serviceRegister) {
        serviceRegister.start(uploadListManager);
        
        // Add setting listener to clear finished uploads.  When set, we clear
        // finished uploads and hide the "clear finished" button.
        SharingSettings.CLEAR_UPLOAD.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        boolean clearUploads = SharingSettings.CLEAR_UPLOAD.getValue();
                        if (clearUploads) {
                            clearFinished();
                        }
                        if (clearFinishedButton != null) {
                            clearFinishedButton.setVisible(!clearUploads);
                        }
                    }
                });
            }
        });
        
        // Add list listener to enable "clear finished" button.
        EventList<UploadItem> doneList = GlazedListsFactory.filterList(
                uploadListManager.getSwingThreadSafeUploads(), 
                new CompleteUploadMatcher());
        doneList.addListEventListener(new ListEventListener<UploadItem>() {
            @Override
            public void listChanged(ListEvent<UploadItem> listChanges) {
                if (clearFinishedButton != null) {
                    clearFinishedButton.setEnabled(listChanges.getSourceList().size() > 0);
                }
            }
        });
    }
    
    /**
     * Returns the component of this mediator.
     */
    public JComponent getComponent() {
        if (uploadPanel == null) {
            uploadPanel = createUploadPanel();
        }
        return uploadPanel;
    }
    
    /**
     * Creates a display panel containing the upload table.
     */
    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        uploadTable = uploadTableFactory.create(this);
        uploadTable.setTableHeader(null);
        
        JScrollPane scrollPane = new JScrollPane(uploadTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Returns a list of active upload items.
     */
    public EventList<UploadItem> getActiveList() {
        if (activeList == null) {
            activeList = GlazedListsFactory.filterList(
                    uploadListManager.getSwingThreadSafeUploads(), 
                    new ActiveUploadMatcher());
        }
        return activeList;
    }
    
    /**
     * Returns a list of header buttons.
     */
    public List<JButton> getHeaderButtons() {
        if (headerButtons == null) {
            clearFinishedButton = new HyperlinkButton(new ClearFinishedAction());
            clearFinishedButton.setVisible(!SharingSettings.CLEAR_UPLOAD.getValue());
            
            headerButtons = new ArrayList<JButton>();
            headerButtons.add(clearFinishedButton);
        }
        
        return headerButtons;
    }
    
    /**
     * Returns the header popup menu associated with the uploads table.
     */
    public JPopupMenu getHeaderPopupMenu() {
        if (headerPopupMenu == null) {
            headerPopupMenu = new UploadHeaderPopupMenu(this, torrentManager, transferTrayNavigator);
        }
        return headerPopupMenu;
    }
    
    /**
     * Returns a sorted list of uploads.
     */
    public EventList<UploadItem> getUploadList() {
        return sortedList;
    }
    
    /**
     * Returns a list of selected upload items.
     */
    public List<UploadItem> getSelectedUploads() {
        if (uploadTable != null) {
            return uploadTable.getSelectedItems();
        } else {
            return Collections.emptyList();
        }
    }
    
    /**
     * Returns true if the uploads list is sorted in ascending order.
     */
    public boolean isSortAscending() {
        return SwingUiSettings.UPLOAD_SORT_ASCENDING.getValue();
    }
    
    /**
     * Returns the sort key for the uploads list.
     */
    public SortOrder getSortOrder() {
        try {
            String sortKey = SwingUiSettings.UPLOAD_SORT_KEY.get();
            return SortOrder.valueOf(sortKey);
        } catch (IllegalArgumentException ex) {
            // Return default order if setting is invalid.
            return SortOrder.ORDER_STARTED;
        }
    }
    
    /**
     * Sets the sort key and direction on the uploads list.
     */
    public void setSortOrder(SortOrder sortOrder, boolean ascending) {
        // Save sort settings.
        SwingUiSettings.UPLOAD_SORT_KEY.set(sortOrder.toString());
        SwingUiSettings.UPLOAD_SORT_ASCENDING.setValue(ascending);
        
        // Apply sort order.
        sortedList.setComparator(new OrderedComparator<UploadItem>(
                getSortComparator(sortOrder), ascending));
    }
    
    /**
     * Returns a comparator for the specified sort key and direction.
     */
    private Comparator<UploadItem> getSortComparator(SortOrder sortOrder) {
        switch (sortOrder) {
        case ORDER_STARTED:
            return new OrderStartedComparator();
        case NAME:
            return new NameComparator();
        case PROGRESS:
            return new ProgressComparator();
        case TIME_REMAINING:
            return new TimeRemainingComparator();
        case SPEED:
            return new SpeedComparator();
        case STATUS:
            return new StateComparator();
        case FILE_TYPE:
            return new CategoryComparator();
        case FILE_EXTENSION:
            return new FileExtensionComparator();
        case USER_NAME:
            return new HostNameComparator();
        default:
            throw new IllegalArgumentException("Unknown SortOrder: " + sortOrder);
        }
    }
    
    /**
     * Returns true if any uploads may be paused.
     */
    public boolean hasPausable() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().readLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isPausable(item)) return true;
            }
        } finally {
            uploadList.getReadWriteLock().readLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if any uploads may be resumed.
     */
    public boolean hasResumable() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().readLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isResumable(item)) return true;
            }
        } finally {
            uploadList.getReadWriteLock().readLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if any uploads are in an error state.
     */
    public boolean hasErrors() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().readLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (item.getState().isError()) return true;
            }
        } finally {
            uploadList.getReadWriteLock().readLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if any uploads are torrents.
     */
    public boolean hasTorrents() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().readLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (item.getUploadItemType() == UploadItemType.BITTORRENT) return true;
            }
        } finally {
            uploadList.getReadWriteLock().readLock().unlock();
        }
        return false;
    }
    
    /**
     * Returns true if the specified upload item is a browse item.
     */
    public static boolean isBrowseHost(UploadItem uploadItem) {
        UploadState state = uploadItem.getState();
        return (state == UploadState.BROWSE_HOST) || (state == UploadState.BROWSE_HOST_DONE);
    }
    
    /**
     * Returns true if the specified upload item may be paused.
     */
    public static boolean isPausable(UploadItem uploadItem) {
        return (uploadItem.getUploadItemType() == UploadItemType.BITTORRENT) &&
            (uploadItem.getState() == UploadState.UPLOADING);
    }
    
    /**
     * Returns true if the specified upload item may be resumed.
     */
    public static boolean isResumable(UploadItem uploadItem) {
        return (uploadItem.getUploadItemType() == UploadItemType.BITTORRENT) &&
            (uploadItem.getState() == UploadState.PAUSED);
    }
    
    /**
     * Returns true if the specified upload item may be removed.
     */
    public static boolean isRemovable(UploadItem uploadItem) {
        UploadState state = uploadItem.getState();
        return state.isFinished() || state.isError();
    }
    
    /**
     * Cancels the specified upload item.  The method prompts the user to 
     * cancel torrent uploads.  If <code>remove</code> is true, the cancelled 
     * item is also removed from the list.
     */
    public void cancel(UploadItem uploadItem, boolean remove) {
        cancel(uploadItem, remove, true);
    }
    
    /**
     * Cancels the specified upload item.  If <code>prompt</code> is true the method 
     * prompts the user to cancel torrent uploads.  If <code>remove</code> is true, the 
     * cancelled item is also removed from the list.
     */
    public void cancel(UploadItem uploadItem, boolean remove, boolean prompt) {
        boolean approved = true;
        
        // For torrents, determine cancel approval based on torrent status and
        // user prompt.  There are various reasons the user will not want the 
        // cancel to go through. 
        // 1) If the torrent is still downloading, the upload cannot be cancelled 
        //    without cancelling the download.
        // 2) If the torrent is seeding, but the seed ratio is low, the user may 
        //    wish to seed to at least 100% to be a good samaritan. 
        if (uploadItem.getUploadItemType() == UploadItemType.BITTORRENT) {
            if (!uploadItem.isStarted()) {
                approved = false;
            } else if (prompt && !uploadItem.isFinished()) {
                approved = promptUser(I18n.tr("If you stop this upload, the torrent download will stop.  Are you sure you want to do this?"));
            } else if (prompt && uploadItem.getSeedRatio() < 1.0f) {
                approved = promptUser(I18n.tr("Are you sure you want to stop this upload?"));
            }
        }
        
        // Cancel upload if approved, and remove from list if specified.
        if (approved) {
            uploadItem.cancel();
            if (remove) {
                remove(uploadItem);
            }
        }
    }
    
    /**
     * Removes the specified upload item from the upload list.
     */
    public void remove(UploadItem uploadItem) {
        uploadListManager.remove(uploadItem);
    }
    
    /**
     * Displays a Yes/No prompt to the user with the specified message, and 
     * returns true if the user presses Yes.
     */
    private boolean promptUser(String message) {
        return FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(), 
                new MultiLineLabel(message, 400), I18n.tr("Uploads"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    
    /**
     * Cancels all uploads.
     */
    public void cancelAll() {
        List<UploadItem> uploadList = new ArrayList<UploadItem>(getUploadList());
        for (UploadItem item : uploadList) {
            cancel(item, false, false);
        }
    }
    
    /**
     * Cancels all uploads in an error state.
     */
    public void cancelAllError() {
        List<UploadItem> uploadList = new ArrayList<UploadItem>(getUploadList());
        for (UploadItem item : uploadList) {
            if (item.getState().isError()) cancel(item, false, false);
        }
    }
    
    /**
     * Cancels all torrent uploads.
     */
    public void cancelAllTorrents() {
        List<UploadItem> uploadList = new ArrayList<UploadItem>(getUploadList());
        for (UploadItem item : uploadList) {
            if (item.getUploadItemType() == UploadItemType.BITTORRENT) cancel(item, false, false);
        }
    }
    
    /**
     * Clears all finished uploads.
     */
    private void clearFinished() {
        uploadListManager.clearFinished();
    }
    
    /**
     * Pauses all uploads that can be paused.
     */
    public void pauseAll() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().readLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isPausable(item)) item.pause();
            }
        } finally {
            uploadList.getReadWriteLock().readLock().unlock();
        }
    }
    
    /**
     * Resumes all uploads that can be resumed.
     */
    public void resumeAll() {
        EventList<UploadItem> uploadList = getUploadList();
        uploadList.getReadWriteLock().readLock().lock();
        try {
            for (UploadItem item : uploadList) {
                if (isResumable(item)) item.resume();
            }
        } finally {
            uploadList.getReadWriteLock().readLock().unlock();
        }
    }
    
    /**
     * Action to clear all finished uploads.
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
	 * Returns true if the UploadItem is currently active, false otherwise.
	 */
    private class ActiveUploadMatcher implements Matcher<UploadItem> {
        @Override
        public boolean matches(UploadItem item) {
            if (item == null) return false;
            
            UploadState state = item.getState();
            return state == UploadState.QUEUED || state == UploadState.UPLOADING;
        }
    }
    
    /**
	 * Return true if the UploadItem is in a compelte state, false otherwise.
	 */
    private class CompleteUploadMatcher implements Matcher<UploadItem> {
        @Override
        public boolean matches(UploadItem item) {
            if (item == null) return false;
            
            UploadState state = item.getState();
            return state == UploadState.DONE 
                    || state == UploadState.LIMIT_REACHED
                    || state == UploadState.CANCELED
                    || state == UploadState.BROWSE_HOST
                    || state == UploadState.BROWSE_HOST_DONE
                    || state == UploadState.REQUEST_ERROR;
        }
    }
    
    private static class OrderStartedComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) { 
            if (o1 == o2) return 0;
            return (int) (o1.getStartTime() - o2.getStartTime());
        }      
    }

    private static class NameComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            String name1 = o1.getFileName();
            String name2 = o2.getFileName();
            return Objects.compareToNullIgnoreCase(name1, name2, false);
        }
    }

    private static class ProgressComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            int pct1 = getProgressPct(o1);
            int pct2 = getProgressPct(o2);
            return (pct1 - pct2);
        }
        
        private int getProgressPct(UploadItem item) {
            // browses have no file size so sort them together below file uploads
            if(item.getFileSize() <= 0)
                return -1;
            return (int) (100 * item.getTotalAmountUploaded() / item.getFileSize());
        }
    }
    
    private static class TimeRemainingComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            long time1 = o1.getRemainingUploadTime();
            long time2 = o2.getRemainingUploadTime();
            return (int) (time1 - time2);
        }
    }

    private static class SpeedComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            float speed1 = o1.getUploadSpeed();
            float speed2 = o2.getUploadSpeed();
            return (int) (speed1 - speed2);
        }
    }

    private static class StateComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            int value1 = getSortValue(o1.getState());
            int value2 = getSortValue(o2.getState());
            return (value1 - value2);
        }
        
        private int getSortValue(UploadState state) {
            switch (state) {
            case DONE: return 1;
            case UPLOADING: return 2;
            case PAUSED: return 3;
            case QUEUED: return 4;
            case REQUEST_ERROR: return 5;    
            case LIMIT_REACHED: return 5;       
            case CANCELED: return 6;
            case BROWSE_HOST: return 7;
            case BROWSE_HOST_DONE: return 8;
            default:
                throw new IllegalArgumentException("Unknown UploadState: " + state);
            }
        }
    }
    
    private static class CategoryComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            Category cat1 = o1.getCategory();
            Category cat2 = o2.getCategory();
            return cat1.compareTo(cat2);
        }
    }

    private static class FileExtensionComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            
            String name1 = o1.getFileName();
            String name2 = o2.getFileName();
            if (name1 == null) {
                return (name2 == null) ? 0 : -1;
            } else if (name2 == null) {
                return 1;
            }
            
            String ext1 = FileUtils.getFileExtension(name1);
            String ext2 = FileUtils.getFileExtension(name2);
            return Objects.compareToNullIgnoreCase(ext1, ext2, false);
        }
    }
    
    private static class HostNameComparator implements Comparator<UploadItem> {
        @Override
        public int compare(UploadItem o1, UploadItem o2) {
            if (o1 == o2) return 0;
            String name1 = o1.getRenderName();
            String name2 = o2.getRenderName();
            return Objects.compareToNullIgnoreCase(name1, name2, false);
        }
    }
    
    private static class OrderedComparator<T> implements Comparator<T> {
        private final Comparator<T> delegate;
        private final boolean ascending;

        public OrderedComparator(Comparator<T> delegate, boolean ascending) {
            this.delegate = delegate;
            this.ascending = ascending;
        }

        @Override
        public int compare(T o1, T o2) {
            return (ascending ? 1 : -1) * delegate.compare(o1, o2);
        }
    }
}
