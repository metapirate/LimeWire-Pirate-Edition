package org.limewire.core.impl.download.listener;

import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

/**
 * Factory allowing creation of TorrentDownloadListeners.
 */
public interface TorrentDownloadListenerFactory {

    /**
     * Creates a TorrentDownloadListener with the given downloader and
     * DownloadItemList.
     */
    public EventListener<DownloadStateEvent> createListener(Downloader downloader,
            List<DownloadItem> list);

}
