package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentParams;

import com.limegroup.gnutella.downloader.CoreDownloader;

public interface BTDownloader extends CoreDownloader {

    /**
     * Initializes the BTDownloader from a torrent file.
     */
    public void init(TorrentParams params) throws IOException;

    /**
     * Returns the incomplete file for this Downloader.
     */
    File getIncompleteFile();

    /**
     * Returns the torrent file backing this downloader if any. Value may be
     * null.
     */
    File getTorrentFile();

    /**
     * Returns a collection of files representing where the completed files will
     * be from this downloader.
     */
    public Collection<File> getCompleteFiles();

    /**
     * Returns the Torrent for this BTDownloader.
     */
    Torrent getTorrent();

}