package org.limewire.core.impl.download.listener;

import java.io.File;

import org.limewire.core.impl.itunes.ItunesMediator;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

/**
 * Listens for the completion of downloads, adding completed ituens supported downloads to the itunes library.
 */
public class ItunesDownloadListener implements EventListener<DownloadStateEvent> {
    private final Downloader downloader;
    private final ItunesMediator itunesMediator;

    @Inject
    public ItunesDownloadListener(@Assisted Downloader downloader, ItunesMediator itunesMediator) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.itunesMediator = itunesMediator;
        if(downloader.getState() == DownloadState.COMPLETE) {
            if(downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStateEvent((CoreDownloader)downloader, DownloadState.COMPLETE));
            }
        }
    }
    @Override
    public void handleEvent(DownloadStateEvent event) {
        DownloadState downloadStatus = event.getType();
        if(DownloadState.COMPLETE == downloadStatus) {
            File saveFile = downloader.getSaveFile();
            if(saveFile != null) {
                if (iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue()) {
                    itunesMediator.addSong(saveFile);
                }
            }
        }
    }
}