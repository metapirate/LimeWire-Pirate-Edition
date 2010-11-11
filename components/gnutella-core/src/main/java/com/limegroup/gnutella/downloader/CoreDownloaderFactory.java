package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.limewire.bittorrent.TorrentParams;
import org.limewire.core.api.download.DownloadException;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;

import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

/**
 * Constructs all kinds of {@link CoreDownloader CoreDownloaders}.<p>
 * 
 * This handles creating downloads from data as well as from mementos
 * of prior downloads.
 */
public interface CoreDownloaderFactory {

    public ManagedDownloader createManagedDownloader(RemoteFileDesc[] files,
            GUID originalQueryGUID, File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException;

    public MagnetDownloader createMagnetDownloader(MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws DownloadException;

    public ResumeDownloader createResumeDownloader(File incompleteFile, String name, long size)
            throws DownloadException;

    /**
     * Creates the appropriate kind of downloader from a given DownloadMemento.
     */
    public CoreDownloader createFromMemento(DownloadMemento memento) throws InvalidDataException;

    /**
     * Creates a downloader to get the torrent file at the given url.
     */
    public BTTorrentFileDownloader createTorrentFileDownloader(URI torrentURI, boolean overwrite);

    /**
     * Creates a BTDownloader from the provided Torrent Parameters.  
     */
    public BTDownloader createBTDownloader(TorrentParams params) throws IOException; 
}
