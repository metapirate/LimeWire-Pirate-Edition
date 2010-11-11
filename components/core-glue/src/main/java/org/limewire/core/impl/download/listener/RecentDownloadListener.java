package org.limewire.core.impl.download.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

/**
 * Listens for the completion of downloads, adding completed downloads to the
 * DownloadSettings.RECENT_DOWNLOADS list.
 */
public class RecentDownloadListener implements EventListener<DownloadStateEvent> {
    private static final int DEFAULT_MAX_TRACKED_DOWNLOADS = 10;

    private final Downloader downloader;
    private final int maxTrackedDownloads; 

    public RecentDownloadListener(Downloader downloader, int maxTrackedDownloads) {
        assert maxTrackedDownloads > 0;
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.maxTrackedDownloads = maxTrackedDownloads;
        
        if (downloader.getState() == DownloadState.COMPLETE) {
            if (downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStateEvent((CoreDownloader) downloader,
                        DownloadState.COMPLETE));
            }
        }
    }
    
    public RecentDownloadListener(Downloader downloader) {
        this(downloader, DEFAULT_MAX_TRACKED_DOWNLOADS);
    }

    @Override
    public void handleEvent(DownloadStateEvent event) {
        // TODO don't do anything for torrent downloads?
        DownloadState downloadStatus = event.getType();
        
        if (DownloadState.COMPLETE == downloadStatus) {
            File saveFile = downloader.getSaveFile();
            if (saveFile != null) {
                if (DownloadSettings.REMEMBER_RECENT_DOWNLOADS.getValue()) {
                    synchronized (RecentDownloadListener.class) {
                        List<File> files;
                        synchronized (DownloadSettings.RECENT_DOWNLOADS) {
                            files = new ArrayList<File>(DownloadSettings.RECENT_DOWNLOADS.get());
                        }
                        files.add(saveFile);
                        Collections.sort(files, new FileDateLeastToMostRecentComparator());
                        while(files.size() > maxTrackedDownloads) {
                            files.remove(0);
                        }
                        DownloadSettings.RECENT_DOWNLOADS.set(new HashSet<File>(files));
                    }
                }
            }
        }
    }
    
    /**
     * Orders files from least to most recent.
     */
    private static class FileDateLeastToMostRecentComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return Long.valueOf(o1.lastModified()).compareTo(Long.valueOf(o2.lastModified()));
        }
    }
}