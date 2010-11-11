package com.limegroup.gnutella;

import org.limewire.listener.DefaultDataTypeEvent;
import com.limegroup.gnutella.downloader.CoreDownloader;

public class DownloadManagerEvent extends DefaultDataTypeEvent<CoreDownloader, DownloadManagerEvent.Type> {

    public enum Type {
        ADDED,
        REMOVED
    }
    
    public DownloadManagerEvent(CoreDownloader data, Type event) {
        super(data, event);
    }

}
