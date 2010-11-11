package com.limegroup.gnutella.downloader;

import java.io.File;

import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.ListenerSupport;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

/**
 * Extends the {@link Downloader} interface to provide more functionality,
 * allowing the download to be more tightly managed.
 */
public interface CoreDownloader extends Downloader, ListenerSupport<DownloadStateEvent> {

    /**
     * Sets the inactive priority of this download.
     */
    public void setInactivePriority(int priority);

    /**
     * Returns the guid that was used for query from which this download was
     * triggered, or null.
     */
    public GUID getQueryGUID();

    /**
     * Starts the download.
     */
    public void startDownload();

    /**
     * @return whether the download is still alive and cannot be restarted.
     */
    public boolean isAlive();

    /**
     * @return whether it makes sense to restart this download.
     */
    public boolean shouldBeRestarted();

    /**
     * @return whether the download should be removed from the waiting list.
     */
    public boolean shouldBeRemoved();

    /**
     * Handles state changes and other operations while inactive.
     */
    public void handleInactivity();

    public boolean isQueuable();

    /**
     * Cleans up any resources before this downloader is completely disposed.
     */
    public void finish();

    public boolean conflicts(URN urn, long fileSize, File... files);

    public boolean conflictsWithIncompleteFile(File incomplete);

    public boolean conflictsSaveFile(File saveFile);

    public void initialize();

    /**
     * Returns the type of download.
     */
    public DownloaderType getDownloadType();

    /** Constructs a memento that will be used for serialization. */
    DownloadMemento toMemento();

    /**
     * Initializes this object from the given memento.
     * 
     * @throws InvalidDataException if the memento contains invalid data.
     */
    void initFromMemento(DownloadMemento memento) throws InvalidDataException;

    /**
     * Indicator of whether this downloader supports a memento or not.
     */
    public boolean isMementoSupported();

}