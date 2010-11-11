package org.limewire.ui.swing.mainframe;

import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * UI container for the tray displayed along the bottom of the application
 * window.  BottomPanel is used to present the Downloads and Uploads tables.
 */
@Singleton
public class BottomPanel extends JPanel {
    public enum TabId {
        DOWNLOADS, UPLOADS
    }
    
    @Resource private int preferredHeight;
    
    private final DownloadMediator downloadMediator;
    private final UploadMediator uploadMediator;
    
    private CardLayout cardLayout;
    
    /**
     * Constructs a BottomPanel with the specified components.
     */
    @Inject
    public BottomPanel(DownloadMediator downloadMediator,
            UploadMediator uploadMediator) {
        this.downloadMediator = downloadMediator;
        this.uploadMediator = uploadMediator;
        
        GuiUtils.assignResources(this);
        
        initializeComponents();
    }
    
    /**
     * Registers listeners on the downloads/uploads mediators.
     */
    @Inject
    void register() {
        downloadMediator.getDownloadList().addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                if (downloadMediator.getDownloadList().size() == 0) {
                    hideWhenNoTransfers();
                }
            }
        });
        
        uploadMediator.getUploadList().addListEventListener(new ListEventListener<UploadItem>() {
            @Override
            public void listChanged(ListEvent<UploadItem> listChanges) {
                if (uploadMediator.getUploadList().size() == 0) {
                    hideWhenNoTransfers();
                }
            }
        });
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initializeComponents() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        
        int savedHeight = SwingUiSettings.BOTTOM_TRAY_SIZE.getValue();
        int height = (savedHeight == 0) ? preferredHeight : savedHeight;
        setPreferredSize(new Dimension(getPreferredSize().width, height));
        
        add(downloadMediator.getComponent(), TabId.DOWNLOADS.toString());
        add(uploadMediator.getComponent(), TabId.UPLOADS.toString());
    }
    
    /**
     * Returns the default preferred height for the bottom tray.
     */
    public int getDefaultPreferredHeight(){
        return preferredHeight;
    }
    
    /**
     * Displays the content associated with the specified tab id.
     */
    public void show(TabId tabId) {
        cardLayout.show(this, tabId.toString());
    }
    
    /**
     * Hides the bottom tray when all transfers are cleared if the option is
     * set to true.
     */
    private void hideWhenNoTransfers() {
        if (SwingUiSettings.HIDE_BOTTOM_TRAY_WHEN_NO_TRANSFERS.getValue()) {
            // Determine whether downloads/uploads tables should be visible.
            boolean showDownloads = SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue() &&
                                    (downloadMediator.getDownloadList().size() > 0);
            boolean showUploads = SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue() && 
                                    UploadSettings.SHOW_UPLOADS_IN_TRAY.getValue() && 
                                    (uploadMediator.getUploadList().size() > 0);
            
            // If both tables empty, clear transfer settings to hide bottom tray.
            if (!(showDownloads || showUploads)) {
                SwingUiSettings.SHOW_TRANSFERS_TRAY.setValue(false);
            }
        }
    }
}
