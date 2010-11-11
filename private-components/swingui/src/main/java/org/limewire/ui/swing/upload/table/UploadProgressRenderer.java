package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.transfer.TransferProgressRenderer;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

/**
 * Cell renderer for the progress column in the Uploads table.
 */
class UploadProgressRenderer extends TransferProgressRenderer {

    /**
     * Constructs an UploadProgressRenderer.
     */
    public UploadProgressRenderer(ProgressBarDecorator progressBarDecorator) {
        super(progressBarDecorator);
    }

    @Override
    protected void updateProgress(Object value, int columnWidth) {
        if (value instanceof UploadItem) {
            UploadItem item = (UploadItem) value;
            
            if (UploadItemType.GNUTELLA == item.getUploadItemType()) {
                // Show progress for Gnutella uploads.
                progressBar.setVisible((item.getState() == UploadState.UPLOADING) && 
                        (columnWidth > resources.getProgressBarCutoffWidth()));
                if (progressBar.isVisible()) {
                    long size = item.getFileSize();
                    progressBar.setValue((size > 0) ? 
                            (int) (100 * item.getTotalAmountUploaded() / size) : 0);
                }
                
            } else {
                // Progress not shown for torrents.
                progressBar.setVisible(false);
            }
            
        } else {
            progressBar.setVisible(false);
        }
        
        progressBar.setEnabled(true);
    }
    
    @Override
    protected void updateTime(Object value) {
        if (value instanceof UploadItem) {
            UploadItem item = (UploadItem) value;
            
            if (UploadItemType.GNUTELLA == item.getUploadItemType()) {
                // Show time left for Gnutella uploads.
                if ((item.getState() == UploadState.UPLOADING) && 
                        (item.getRemainingUploadTime() <= Long.MAX_VALUE - 1000)) {
                    timeLabel.setText(I18n.tr("{0} left", CommonUtils.seconds2time(
                            item.getRemainingUploadTime())));
                    timeLabel.setMinimumSize(timeLabel.getPreferredSize());
                    timeLabel.setVisible(true);
                } else {
                    timeLabel.setVisible(false);
                }
                
            } else {
                // Time left not shown for torrents.
                timeLabel.setVisible(false);
            }
            
        } else {
            timeLabel.setVisible(false);
        }
    }
}
