package org.limewire.ui.swing.downloads.table.renderer;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.malware.AntivirusUpdateType;
import org.limewire.ui.swing.transfer.TransferTitleRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertiableFileUtils;

import com.google.inject.Inject;

/**
 * Cell renderer for the title column in the Downloads table.
 */
public class DownloadTitleRenderer extends TransferTitleRenderer {

    @Resource private Icon antivirusIcon;
    @Resource private Icon warningIcon;
    @Resource private Icon downloadingIcon;
    
    private CategoryIconManager categoryIconManager;
    
    @Inject
    public DownloadTitleRenderer(CategoryIconManager categoryIconManager) {
        this.categoryIconManager = categoryIconManager;
        
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected Icon getIcon(Object value) {
        if (!(value instanceof DownloadItem)) {
            return null;
        }
        DownloadItem item = (DownloadItem) value;
        
        if (item.getDownloadItemType() == DownloadItemType.ANTIVIRUS) {
            return antivirusIcon;
        }
        
        switch (item.getState()) {
        case ERROR:
        case DANGEROUS:
        case THREAT_FOUND:
        case SCAN_FAILED:
            return warningIcon;

        case FINISHING:
        case DONE:
            return categoryIconManager.getIcon(item.getCategory());
            
        case SCANNING:
        case SCANNING_FRAGMENT:
            return antivirusIcon;
            
        default:
            return downloadingIcon;
        }
    }
    
    @Override
    protected String getText(Object value) {
        if (!(value instanceof DownloadItem)) {
            return "";
        }
        DownloadItem item = (DownloadItem) value;
        
        switch (item.getDownloadItemType()) {
        case ANTIVIRUS:          
            return getAntivirusText(item);
        case BITTORRENT:
            return I18n.tr("{0} (torrent)", PropertiableFileUtils.getNameProperty(item, true));
        case GNUTELLA:
        default:
            return PropertiableFileUtils.getNameProperty(item, true);
        }
    }
    
    private String getAntivirusText(DownloadItem item) {
        AntivirusUpdateType type = (AntivirusUpdateType)item.getDownloadProperty(DownloadPropertyKey.ANTIVIRUS_UPDATE_TYPE);

        switch (type) {
        case CHECKING:
            return I18n.tr("Checking for AVG Anti-Virus updates");
            
        case FULL:
            return I18n.tr("Updating AVG Anti-Virus");
            
        case INCREMENTAL:
            Integer index = (Integer) item.getDownloadProperty(DownloadPropertyKey.ANTIVIRUS_INCREMENT_INDEX);
            Integer count = (Integer) item.getDownloadProperty(DownloadPropertyKey.ANTIVIRUS_INCREMENT_COUNT);
            // {0}: current update, {1} total number of updates
            return I18n.tr("Updating AVG Anti-Virus definitions - {0} of {1}", index, count);
            
        default:
            return I18n.tr("Updating AVG Anti-Virus definitions");
        }

    }
}
