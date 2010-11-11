package org.limewire.core.impl.download;

import com.limegroup.gnutella.Downloader;

/**
 * Defines a listener for download events. 
 */
public interface DownloadListener {
    
    public void downloadAdded(Downloader downloader);

    public void downloadRemoved(Downloader downloader);
    
    public void downloadsCompleted();
    
}
