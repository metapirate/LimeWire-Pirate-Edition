package org.limewire.ui.swing.downloads.table.renderer;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.transfer.TransferProgressRenderer;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;

/**
 * Cell renderer for the progress column in the Downloads table.
 */
public class DownloadProgressRenderer extends TransferProgressRenderer {

    /**
     * Constructs a DownloadProgressRenderer.
     */
    @Inject
    public DownloadProgressRenderer(ProgressBarDecorator progressBarDecorator) {
        super(progressBarDecorator);
    }
    
    @Override
    protected void updateProgress(Object value, int columnWidth) {
        if (value instanceof DownloadItem) {
            DownloadItem item = (DownloadItem) value;
            DownloadState state = item.getState();
        
            progressBar.setValue(item.getPercentComplete());
            progressBar.setVisible(columnWidth > resources.getProgressBarCutoffWidth() &&
                    (state == DownloadState.DOWNLOADING ||
                            state == DownloadState.PAUSED ||
                            state == DownloadState.SCANNING ||
                            state == DownloadState.SCANNING_FRAGMENT));
            progressBar.setEnabled(state != DownloadState.PAUSED);
        
        } else {
            progressBar.setValue(0);
            progressBar.setVisible(false);
            progressBar.setEnabled(true);
        }
    }
    
    @Override
    protected void updateTime(Object value) {
        if (value instanceof DownloadItem) {
            DownloadItem item = (DownloadItem) value;
            DownloadState state = item.getState();

            if (state == DownloadState.SCANNING) {
                timeLabel.setText(I18n.tr("Finalizing..."));
                timeLabel.setMinimumSize(timeLabel.getPreferredSize());
                timeLabel.setVisible(true);
                
            } else if (state != DownloadState.DOWNLOADING || 
                    item.getRemainingDownloadTime() > Long.MAX_VALUE - 1000) {
                timeLabel.setVisible(false);
                
            } else {
                timeLabel.setText(I18n.tr("{0} left", CommonUtils.seconds2time(item
                        .getRemainingDownloadTime())));
                timeLabel.setMinimumSize(timeLabel.getPreferredSize());
                timeLabel.setVisible(true);
            }

        } else {
            timeLabel.setVisible(false);
        }
    }
}
