package org.limewire.core.impl.download.listener;

import com.limegroup.gnutella.Downloader;

public interface ItunesDownloadListenerFactory {
    public ItunesDownloadListener createListener(Downloader downloader);
}
