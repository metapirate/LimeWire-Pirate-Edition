package org.limewire.bittorrent;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.listener.EventListener;

/**
 * Class representing the torrent being downloaded/uploaded.
 * 
 * When multiple method calls need to be called in tandem and must be valid
 * across calls the provided getLock method can be used to get a lock, so the
 * calls will stay consistent across the multiple methods.
 * 
 * One thing to note, when sharing locks between the torrent manager and
 * individual torrents. The torrent manager must be locked first, before any
 * torrents are lock. If this is not adhered to then a deadlock situacion might
 * occur.
 */
public interface Torrent {

    /**
     * Returns the name of this torrent.
     */
    public String getName();

    /**
     * Starts the torrent.
     */
    public void start();

    /**
     * Returns the torrent file backing this torrent if any exist. Null or a
     * non-existent file can be returned.
     */
    public File getTorrentFile();

    /**
     * Returns the fastResume file backing this torrent if any. Null or a
     * non-existent file can be returned.
     */
    public File getFastResumeFile();

    /**
     * Moves the torrent to the specified directory.
     */
    public void moveTorrent(File directory);

    /**
     * Pauses the torrent.
     */
    public void pause();

    /**
     * Resumes the torrent from a paused state.
     */
    public void resume();

    /**
     * Returns the download rate in bytes/second.
     */
    public float getDownloadRate();

    /**
     * Returns a hexString representation of this torrents sha1.
     */
    public String getSha1();

    /**
     * Returns true if this torrent is paused, false otherwise.
     */
    public boolean isPaused();

    /**
     * Returns true if this torrent is finished, false otherwise.
     */
    public boolean isFinished();

    /**
     * Returns true if the torrent has been started, false otherwise.
     */
    public boolean isStarted();

    /**
     * Returns the tracker uris to this torrent.  Never Null.
     */
    public List<URI> getTrackerURIS();

    /**
     * Returns the list of trackers being used for this torrent.
     */
    public List<TorrentTracker> getTrackers();
    
    /**
     * Returns the number of peers in this torrents swarm.
     */
    public int getNumPeers();

    /**
     * Returns the root data file for this torrent.
     */
    public File getTorrentDataFile();

    /**
     * Stops the torrent by removing it from the torrent manager.
     */
    public void stop();

    /**
     * Returns the total number of byte uploaded for this torrent.
     */
    public long getTotalUploaded();

    /**
     * Returns current number of upload connections.
     */
    public int getNumUploads();

    /**
     * Returns the current upload rate in bytes/second.
     */
    public float getUploadRate();

    /**
     * Returns the current seed ratio, with 1.0 being at 100%.
     */
    public float getSeedRatio();

    /**
     * Returns true if this torrent has been canceled, false otherwise.
     */
    public boolean isCancelled();

    /**
     * Returns a status object representing this torrents internal state.
     */
    public TorrentStatus getStatus();

    /**
     * Updates this torrents internal state using the given LibTorrentStatus
     * object.
     */
    public void updateStatus(TorrentStatus torrentStatus);

    /**
     * Updates this torrents internal state using the given LibTorrentAlerts.
     */
    public void handleFastResumeAlert(TorrentAlert alert);

    /**
     * Removes the listener from the torrent. Returning true if the listener
     * attached and removed.
     */
    boolean removeListener(EventListener<TorrentEvent> listener);

    /**
     * Adds a listener to this torrent.
     */
    void addListener(EventListener<TorrentEvent> listener);

    /**
     * Returns the number of connections this torrent has.
     */
    public int getNumConnections();

    /**
     * Returns true if this is a private torrent.
     */
    public boolean isPrivate();

    /**
     * Returns a list of TorrentFileEntry containing an entry for each file in
     * this torrent.
     */
    public List<TorrentFileEntry> getTorrentFileEntries();

    /**
     * Returns a list of currently connected peers for this torrent.
     */
    public List<TorrentPeer> getTorrentPeers();

    /**
     * Returns true if the torrent is automanaged.
     */
    public boolean isAutoManaged();

    /**
     * Sets whether or not this torrent is automanaged. For an explanation of
     * automanagement see
     * http://www.rasterbar.com/products/libtorrent/manual.html#queuing
     * 
     * Basically it means that the torrent will be managed by libtorrent. Every
     * polling period queued and active torrents are checked to see if they
     * should be given some active time to allow for seeding/downloading.
     * Automanaged torrents adhere to limits for total torrents allowed active,
     * total seeds, etc.
     */
    public void setAutoManaged(boolean autoManaged);

    /**
     * Sets the priority for the specified TorrentFileEntry.
     */
    public void setTorrenFileEntryPriority(TorrentFileEntry torrentFileEntry, int priority);

    /**
     * Returns the filesystem path for the specified torrentFileEntry.
     */
    public File getTorrentDataFile(TorrentFileEntry torrentFileEntry);

    /**
     * Returns true if this torrent has metadata yet or not.
     */
    public boolean hasMetaData();

    /**
     * Returns the TorrentInfo object for this torrent, can be null when the
     * torrent does no yet have its metadata loaded.
     */
    public TorrentInfo getTorrentInfo();

    /**
     * Sets a property on the torrent to the given value.
     */
    public void setProperty(String key, Object value);

    /**
     * Gets the value for the given property for this torrent. The supplied
     * default value is used if unset.
     */
    public <T> T getProperty(String key, T defaultValue);

    /**
     * Returns true of this torrent is still considered valid.
     */
    public boolean isValid();

    /**
     * Returns the time in milliseconds that this torrent was started in the
     * current session.
     */
    public long getStartTime();

    /**
     * Sets the new path for the torrent file.
     */
    public void setTorrentFile(File torrentFile);

    /**
     * Sets the new path for the fast resume file.
     */
    public void setFastResumeFile(File fastResumeFile);

    /**
     * Forces the torrent to reannounce itself to its tracker.
     */
    public void forceReannounce();

    /**
     * Makes the torrent rescrape its tracker to get updated statistics for the
     * torrent.
     */
    public void scrapeTracker();

    /**
     * Tells the torrent to save its fast resume data to the given fastResume
     * file.
     */
    public void saveFastResumeData();

    /**
     * Returns a lock for this torrent. Locking is only required if state needs
     * to be consistent across multiple method calls. Otherwise locking is done
     * internally for single method calls.
     */
    public Lock getLock();

    /**
     * Gets the pieces state data thus far.
     */
    public TorrentPiecesInfo getPiecesInfo();

    /**
     * Adds a tracker to a torrent at a given tier.
     */
    public void addTracker(String url, int tier);
    
    
    /**
     * Removes any tracker matching the url and tier. 
     */
    public void removeTracker(String url, int tier);
    
    /**
     * Returns true if this file is editable. A non-editable Torrent
     * is one that is constructed of XML data.
     */
    public boolean isEditable();
    
    /**
     * Returns the maximum download bandwidth this Torrent will use.
     */
    public int getMaxDownloadBandwidth(); 
    
    /**
     * Sets the maximum download bandwidth this Torrent will use.
     */
    public void setMaxDownloadBandwidth(int value);
    
    /**
     * Returns the maximum upload bandwidth this Torrent will use.
     */
    public int getMaxUploadBandwidth();
    
    /**
     * Sets the maximum upload bandwidth this Torrent can use.
     */
    public void setMaxUploadBandwidth(int value);

    /**
     * @return The total size of all the entries in the torrent
     */
    public long getTotalPayloadSize(); 

}
