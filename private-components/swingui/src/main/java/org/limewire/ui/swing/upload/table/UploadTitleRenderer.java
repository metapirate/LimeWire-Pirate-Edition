package org.limewire.ui.swing.upload.table;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadItem.BrowseType;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.transfer.TransferTitleRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer for the title column in the Uploads table.
 */
class UploadTitleRenderer extends TransferTitleRenderer {

    @Resource private Icon friendBrowseHostIcon;
    @Resource private Icon p2pBrowseHostIcon;
    
    private final CategoryIconManager iconManager;
    
    /**
     * Constructs an UploadTitleRenderer.
     */
    public UploadTitleRenderer(CategoryIconManager iconManager) {
        this.iconManager = iconManager;
        
        GuiUtils.assignResources(this);
    }

    @Override
    protected Icon getIcon(Object value) {
        if (!(value instanceof UploadItem)) {
            return null;
        }
        UploadItem uploadItem = (UploadItem) value;
        
        switch (uploadItem.getState()) {
        case UPLOADING:
            return iconManager.getIcon(uploadItem.getCategory());

        case BROWSE_HOST:
        case BROWSE_HOST_DONE:
            if (uploadItem.getBrowseType() == BrowseType.FRIEND) {
                return friendBrowseHostIcon;
            } else {
                return p2pBrowseHostIcon;
            }

        default:
            return iconManager.getIcon(uploadItem.getCategory());
        }
    }

    @Override
    protected String getText(Object value) {
        if (!(value instanceof UploadItem)) {
            return "";
        }
        UploadItem uploadItem = (UploadItem) value;
        
        switch (uploadItem.getState()) {
        case BROWSE_HOST:
        case BROWSE_HOST_DONE:
            return uploadItem.getRenderName();
            
        default:
            if (uploadItem.getUploadItemType() == UploadItemType.BITTORRENT) {
                return I18n.tr("{0} (torrent)", uploadItem.getFileName());
            } else {
                return uploadItem.getFileName() + " - " + uploadItem.getRenderName();
            }
        }
    }
}
