package com.limegroup.bittorrent;

import java.io.File;
import java.net.URI;

import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.http.HttpClientListener;

/**
 * This downloader allows for downloading a torrent file. It will parse the file
 * into a BTMetaInfo object. The file is shared with the gnutella network if the
 * file is allowed to be shared. This could eventually be moved to a more
 * generic downloader. This is the only downloader that will currently support
 * redirects however. And it is blocking. The downloader is essentially copied
 * from the TorrentFileFetcher with the exception that this does not start the
 * torrent download after downloading the .torrent file. Instead that work is
 * handled by a listener checking for torrent file downloads.
 */
public interface BTTorrentFileDownloader extends HttpClientListener, CoreDownloader {

    /**
     * Initializes the downloader from a URI.
     */
    public void initDownloadInformation(URI torrentURI, boolean overwrite);

    /**
     * Returns the torrent file downloaded by this downloader. Can be null if
     * the download failed.
     */
    public File getTorrentFile();

}