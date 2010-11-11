package com.limegroup.gnutella.downloader;

import org.limewire.listener.DefaultSourceTypeEvent;

import com.limegroup.gnutella.Downloader.DownloadState;

public class DownloadStateEvent extends DefaultSourceTypeEvent<CoreDownloader, DownloadState> {

    public DownloadStateEvent(CoreDownloader source, DownloadState event) {
        super(source, event);
    }

}
