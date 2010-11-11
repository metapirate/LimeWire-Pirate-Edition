package org.limewire.ui.swing.downloads;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.downloads.DownloadMediator.SortOrder;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * PopupMenu for performing actions on the download table.
 */
class DownloadHeaderPopupMenu extends JPopupMenu {

    @Resource
    private Icon upArrow;
    @Resource
    private Icon downArrow;
    
    private final Provider<DownloadMediator> downloadMediator;
    private final Provider<ShowDownloadOptionsAction> showDownloadOptionsAction;
    private final Provider<PauseAllDownloadAction> pauseDownloadAction;
    private final Provider<ResumeAllDownloadAction> resumeDownloadAction;
    private final Provider<CancelAllStalledDownloadAction> cancelStallAction;
    private final Provider<CancelAllErrorDownloadAction> cancelErrorAction;
    private final Provider<CancelAllDownloadsAction> cancelAllAction;
    private final Provider<ShowUploadsInTrayAction> showUploadsInTrayAction;
    
    @Inject
    public DownloadHeaderPopupMenu(Provider<DownloadMediator> downloadMediator, Provider<ShowDownloadOptionsAction> showDownloadOptionsAction,
            Provider<PauseAllDownloadAction> pauseDownloadAction, Provider<ResumeAllDownloadAction> resumeDownloadAction,
            Provider<CancelAllStalledDownloadAction> cancelStallAction,
            Provider<CancelAllErrorDownloadAction> cancelErrorAction, Provider<CancelAllDownloadsAction> cancelAllAction, 
            Provider<ShowUploadsInTrayAction> showUploadsInTrayAction) {
        this.downloadMediator = downloadMediator;
        this.showDownloadOptionsAction = showDownloadOptionsAction;
        this.pauseDownloadAction = pauseDownloadAction;
        this.resumeDownloadAction = resumeDownloadAction;
        this.cancelStallAction = cancelStallAction;
        this.cancelErrorAction = cancelErrorAction;
        this.cancelAllAction = cancelAllAction;
        this.showUploadsInTrayAction = showUploadsInTrayAction;
        
        GuiUtils.assignResources(this);
    }
    
    public void populate() {
        removeAll();
        
        final JCheckBoxMenuItem clearFinishedCheckBox = new JCheckBoxMenuItem(I18n.tr("Clear When Finished"));
        clearFinishedCheckBox.setSelected(SharingSettings.CLEAR_DOWNLOAD.getValue());
        clearFinishedCheckBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                SharingSettings.CLEAR_DOWNLOAD.setValue(clearFinishedCheckBox.isSelected());
            }
        });
        
        add(pauseDownloadAction.get()).setEnabled(downloadMediator.get().hasPausable());
        add(resumeDownloadAction.get()).setEnabled(downloadMediator.get().hasResumable());
        add(createCancelSubMenu());
        addSeparator();
        add(createSortSubMenu());
        addSeparator();
        add(clearFinishedCheckBox);
        addSeparator();
        JCheckBoxMenuItem showUploadsInTray = new JCheckBoxMenuItem(showUploadsInTrayAction.get());
        showUploadsInTray.setSelected(UploadSettings.SHOW_UPLOADS_IN_TRAY.getValue());
        add(showUploadsInTray);
        addSeparator();
        add(showDownloadOptionsAction.get());
    }
    
    private JMenu createCancelSubMenu(){
        JMenu cancelSubMenu = new JMenu(I18n.tr("Cancel"));
        
        cancelSubMenu.add(cancelStallAction.get()).setEnabled(downloadMediator.get().containsState(DownloadState.STALLED));
        cancelSubMenu.add(cancelErrorAction.get()).setEnabled(downloadMediator.get().containsState(DownloadState.ERROR));
        cancelSubMenu.add(cancelAllAction.get()).setEnabled(downloadMediator.get().getDownloadList().size() > 0);

        return cancelSubMenu;
    }

    private JMenu createSortSubMenu(){
        JMenu sortSubMenu = new JMenu(I18n.tr("Sort by"));
        
        JCheckBoxMenuItem orderAdded = new JCheckBoxMenuItem(new SortAction(I18n.tr("Order Added"), SortOrder.ORDER_ADDED));
        JCheckBoxMenuItem name = new JCheckBoxMenuItem(new SortAction(I18n.tr("Name"), SortOrder.NAME));
        JCheckBoxMenuItem progress = new JCheckBoxMenuItem(new SortAction(I18n.tr("Progress"), SortOrder.PROGRESS));
        JCheckBoxMenuItem timeRemaining = new JCheckBoxMenuItem(new SortAction(I18n.tr("Time Left"), SortOrder.TIME_REMAINING));
        JCheckBoxMenuItem speed = new JCheckBoxMenuItem(new SortAction(I18n.tr("Speed"), SortOrder.SPEED));
        JCheckBoxMenuItem status = new JCheckBoxMenuItem(new SortAction(I18n.tr("Status"), SortOrder.STATUS));
        JCheckBoxMenuItem fileType = new JCheckBoxMenuItem(new SortAction(I18n.tr("File Type"), SortOrder.FILE_TYPE));
        JCheckBoxMenuItem extension = new JCheckBoxMenuItem(new SortAction(I18n.tr("File Extension"), SortOrder.EXTENSION));        

        ButtonGroup sortButtonGroup = new ButtonGroup();
        sortButtonGroup.add(orderAdded);
        sortButtonGroup.add(name);
        sortButtonGroup.add(progress);
        sortButtonGroup.add(timeRemaining);
        sortButtonGroup.add(speed);
        sortButtonGroup.add(status);
        sortButtonGroup.add(fileType);
        sortButtonGroup.add(extension);
        
        AbstractButton reverseButton = new JMenuItem(new AbstractAction(I18n.tr("Reverse Order")) {
            {
                putValue(Action.SELECTED_KEY, downloadMediator.get().isSortAscending());
                putValue(Action.SMALL_ICON, downloadMediator.get().isSortAscending() ? downArrow : upArrow);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean direction = !downloadMediator.get().isSortAscending();
                putValue(Action.SMALL_ICON, direction ? downArrow : upArrow);
                downloadMediator.get().setSortOrder(downloadMediator.get().getSortOrder(), direction);
            }
        });
        
        sortSubMenu.add(orderAdded);
        sortSubMenu.add(name);
        sortSubMenu.add(progress);
        sortSubMenu.add(timeRemaining);
        sortSubMenu.add(speed);
        sortSubMenu.add(status);
        sortSubMenu.add(fileType);
        sortSubMenu.add(extension);
        sortSubMenu.addSeparator();
        sortSubMenu.add(reverseButton);
        
        return sortSubMenu;
    }

    private class SortAction extends AbstractAction{
        private final SortOrder order;
    
        public SortAction(String title, SortOrder order){
            super(title);
            this.order = order;
            
            //select this action if its currently sorted on
            putValue(SELECTED_KEY, order == downloadMediator.get().getSortOrder());
        }
      
        @Override
        public void actionPerformed(ActionEvent e) {
            downloadMediator.get().setSortOrder(order, downloadMediator.get().isSortAscending());
        }
    }; 
}
