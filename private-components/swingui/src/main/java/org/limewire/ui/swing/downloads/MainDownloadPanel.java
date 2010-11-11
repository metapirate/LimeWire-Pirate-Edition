package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Application;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.inject.EagerSingleton;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.downloads.table.AVInfoPanel;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Container to display the Downloads table.
 * 
 * <p>Note: This is an EagerSingleton to ensure that register() is called and 
 * the listeners are in place at startup.</p>
 */
@EagerSingleton
public class MainDownloadPanel extends JPanel {  	
    
    public static final String NAME = "MainDownloadPanel";    
        
    private final DownloadMediator downloadMediator;
    private final Provider<DownloadTableFactory> downloadTableFactory;
    private final DownloadListManager downloadListManager;
    private final Provider<PlayerMediator> playerMediator;
    private final Provider<AVInfoPanel> avInfoPanelFactory;
    
    private TrayNotifier notifier;
    private boolean isInitialized = false;
    private DownloadTable table;
    
    
    /**
     * Create the panel.
     */
    @Inject
    public MainDownloadPanel(Provider<DownloadTableFactory> downloadTableFactory, 
            DownloadMediator downloadMediator,
            TrayNotifier notifier, 
            DownloadListManager downloadListManager,
            Provider<PlayerMediator> playerMediator,
            Provider<AVInfoPanel> avInfoPanelFactory) {
        this.downloadMediator = downloadMediator;
        this.downloadTableFactory = downloadTableFactory;
        this.downloadListManager = downloadListManager;
        this.playerMediator = playerMediator;
        this.avInfoPanelFactory = avInfoPanelFactory;
        this.notifier = notifier;

        GuiUtils.assignResources(this);
    }
    
    public void selectAndScrollTo(URN urn) {
        // its possible the table hasn't been initialized yet
        // after the first download starts
        if(table == null) {
            initialize();
        }
        table.selectAndScrollTo(urn);
    }

    
    @Inject
    public void register() {              
        // Add listener for "show downloads" setting.
        SwingUiSettings.SHOW_TRANSFERS_TRAY.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        initialize();
                    }
                });
            } 
        });
        
        //we have to eagerly initialize the table when the SHOW_DOWNLOAD_TRAY setting
        //is set to true on startup, otherwise the table space will be empty and the lines will
        //be put in the first time a download comes in, which looks a little weird
        if (SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue()) {
            initialize();
        }
        
        // Add listener for downloads added and completed.
        downloadListManager.addPropertyChangeListener(new DownloadPropertyListener());
    }
    
    //Lazily initialized - initialize() is called when the first downloadItem is added to the list.  
    private void initialize() {
        if(!isInitialized){
            isInitialized = true;
            setLayout(new BorderLayout());
    
            table = downloadTableFactory.get().create(downloadMediator.getDownloadList());
            table.setTableHeader(null);
            JScrollPane pane = new JScrollPane(table);
            pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            add(pane, BorderLayout.CENTER);
        }
    }
    
    public List<DownloadItem> getSelectedDownloadItems(){
        return table.getSelectedItems();
    }

    /**
     * Listener to handle download added/completed events from the download 
     * list manager.
     */
    private class DownloadPropertyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(DownloadListManager.DOWNLOAD_COMPLETED)) {
                final DownloadItem downloadItem = (DownloadItem) event.getNewValue();
                DownloadState state = downloadItem.getState();
                if(downloadItem.getDownloadItemType() == DownloadItemType.ANTIVIRUS) {
                    // Don't show a popup when an antivirus download completes
                } else if (state == DownloadState.THREAT_FOUND) {
                    avInfoPanelFactory.get().showThreatMessage(downloadItem, true);
                } else if (state == DownloadState.SCAN_FAILED) {
                    avInfoPanelFactory.get().showFailureMessage(downloadItem, true);
                } else if (state == DownloadState.DANGEROUS) {
                    avInfoPanelFactory.get().showDangerMessage(downloadItem, true);
                } else {
                    notifier.showMessage(new Notification(I18n.tr("Download Complete"), downloadItem.getFileName(), 
                            new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ActionMap map = Application.getInstance().getContext().getActionManager()
                            .getActionMap();
                            map.get("restoreView").actionPerformed(e);

                            if (downloadItem.isLaunchable()) {
                                DownloadItemUtils.launch(downloadItem, playerMediator);
                            }
                        }
                    }));
                }
                
                // For LWC-4733. It is possible with extremely small file
                // downloads that the download can complete before initialize()
                // is called. We are planning to refactor so that this won't
                // happen but this null check will prevent the NPE in the release.
                if(table == null) {
                    initialize();
                }

                // the user might be editing one of the cell's while the download completes,
                // i.e. the user might have the mouse hovering over the pause button. (Bug LWC-4317)
                // Let's manually cancel cell editing here after the download completes
                TableCellEditor editor = table.getCellEditor();
                if ( editor != null )
                {
                    editor.cancelCellEditing();
                }
            }
        }
    }
}
