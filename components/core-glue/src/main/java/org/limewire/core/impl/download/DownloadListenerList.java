package org.limewire.core.impl.download;


public interface DownloadListenerList {

    void addDownloadListener(DownloadListener listener);

    void removeDownloadListener(DownloadListener listener);

}
