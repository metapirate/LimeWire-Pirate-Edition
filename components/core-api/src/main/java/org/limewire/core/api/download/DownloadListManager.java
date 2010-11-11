package org.limewire.core.api.download;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.List;

import org.limewire.core.api.URN;
import org.limewire.core.api.magnet.MagnetLink;

import ca.odell.glazedlists.EventList;

/**
 * Defines the manager API for the list of downloads.
 */
public interface DownloadListManager extends ResultDownloader {
    /** Property name for a download added event. */
    public static final String DOWNLOAD_ADDED = "downloadAdded";

    /** Property name for downloads completed event. */
    public static final String DOWNLOADS_COMPLETED = "downloadsCompleted";

    /** Property name for a download completed event. */
    public static final String DOWNLOAD_COMPLETED = "downloadCompleted";

    /**
     * Returns all items currently being downloaded.
     */
    public EventList<DownloadItem> getDownloads();

    /** Returns a Swing-thread safe version of the downloads event list. */
    public EventList<DownloadItem> getSwingThreadSafeDownloads();

    /**
     * Downloads the torrent file at the given uri.
     */
    public DownloadItem addTorrentDownload(URI uri, boolean overwrite) throws DownloadException;
    
    /**
     * Tries to start a torrent download without a torrent file.
     */
    public DownloadItem addTorrentDownload(String name, URN sha1, List<URI> trackers) throws DownloadException;

    /**
     * Opens the given file and starts a downloader based on the information
     * inside of the given file.
     */
    public DownloadItem addTorrentDownload(File file, File saveDirectory, boolean overwrite)
            throws DownloadException;

    /**
     * Return true if the downloader contains the given urn, false otherwise.
     */
    public boolean contains(URN urn);

    /**
     * Downloads the given magnet link.
     */
    public DownloadItem addDownload(MagnetLink magnet, File saveFile, boolean overwrite)
            throws DownloadException;

    /**
     * Adds the specified listener to the list that is notified when a property
     * value changes.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the specified listener from the list that is notified when a
     * property value changes.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Checks for downloads in progress, and fires a property change event if
     * all downloads are completed.
     */
    public void updateDownloadsCompleted();

    /**
     * Clears all completed downloads.
     */
    public void clearFinished();

    /**
     * Returns a download item for the given URN if any, null is returned
     * otherwise.
     */
    DownloadItem getDownloadItem(URN urn);

    /** Removes download item from the list. */
    public void remove(DownloadItem item);

}
