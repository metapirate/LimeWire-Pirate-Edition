package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.limewire.bittorrent.TorrentParams;
import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

@Singleton
public class CoreDownloaderFactoryImpl implements CoreDownloaderFactory {

    private final Provider<ManagedDownloader> managedDownloaderFactory;
    private final Provider<MagnetDownloader> magnetDownloaderFactory;
    private final Provider<ResumeDownloader> resumeDownloaderFactory;
    private final Provider<BTDownloader> btDownloaderFactory;
    private final Provider<BTTorrentFileDownloader> torrentFileDownloaderFactory;

    @Inject
    public CoreDownloaderFactoryImpl(
            Provider<ManagedDownloader> managedDownloaderFactory,
            Provider<MagnetDownloader> magnetDownloaderFactory,
            Provider<ResumeDownloader> resumeDownloaderFactory,
            Provider<BTDownloader> btDownloaderFactory,
            Provider<BTTorrentFileDownloader> torrentFileDownloaderFactory) {
        this.managedDownloaderFactory = managedDownloaderFactory;
        this.magnetDownloaderFactory = magnetDownloaderFactory;
        this.resumeDownloaderFactory = resumeDownloaderFactory;
        this.btDownloaderFactory = btDownloaderFactory;
        this.torrentFileDownloaderFactory = torrentFileDownloaderFactory;
    }

    @Override
    public ManagedDownloader createManagedDownloader(RemoteFileDesc[] files,
            GUID originalQueryGUID, File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException {
        ManagedDownloader md = managedDownloaderFactory.get();
        md.addInitialSources(Arrays.asList(files), fileName);
        md.setQueryGuid(originalQueryGUID);
        md.setSaveFile(saveDirectory, fileName, overwrite);
        return md;
    }

    @Override
    public MagnetDownloader createMagnetDownloader(MagnetOptions magnet, boolean overwrite,
            File saveDirectory, String fileName) throws DownloadException {
        if (!magnet.isGnutellaDownloadable())
            throw new IllegalArgumentException("magnet not downloadable");
        if (fileName == null)
            fileName = magnet.getFileNameForSaving();

        MagnetDownloader md = magnetDownloaderFactory.get();
        md.addInitialSources(null, fileName);
        md.setSaveFile(saveDirectory, fileName, overwrite);
        md.setMagnet(magnet);
        return md;
    }

    @Override
    public ResumeDownloader createResumeDownloader(File incompleteFile, String name, long size)
            throws DownloadException {
        ResumeDownloader rd = resumeDownloaderFactory.get();
        rd.addInitialSources(null, name);
        rd.setSaveFile(null, name, false);
        rd.initIncompleteFile(incompleteFile, size);
        return rd;
    }

    @Override
    public BTDownloader createBTDownloader(TorrentParams params) throws IOException {
        BTDownloader bd = btDownloaderFactory.get();
        bd.init(params);
        return bd;
    }

    @Override
    public BTTorrentFileDownloader createTorrentFileDownloader(URI torrentURI, boolean overwrite) {
        BTTorrentFileDownloader torrentFileDownloader = torrentFileDownloaderFactory.get();
        torrentFileDownloader.initDownloadInformation(torrentURI, overwrite);
        return torrentFileDownloader;
    }

    @Override
    public CoreDownloader createFromMemento(DownloadMemento memento) throws InvalidDataException {
        try {
            Provider<? extends CoreDownloader> coreFactory = providerForMemento(memento);
            CoreDownloader downloader = coreFactory.get();
            downloader.initFromMemento(memento);
            return downloader;
        } catch (Throwable t) {
            throw new InvalidDataException("invalid memento!", t);
        }
    }

    private Provider<? extends CoreDownloader> providerForMemento(DownloadMemento memento)
            throws InvalidDataException {
        switch (memento.getDownloadType()) {
        case BTDOWNLOADER:
            return btDownloaderFactory;
        case MAGNET:
            return magnetDownloaderFactory;
        case MANAGED:
            return managedDownloaderFactory;
        case INNETWORK:
        case STORE:
        case TORRENTFETCHER:
        case MOZILLA:
        default:
            throw new InvalidDataException("invalid memento type: " + memento.getDownloadType());
        }
    }
}
