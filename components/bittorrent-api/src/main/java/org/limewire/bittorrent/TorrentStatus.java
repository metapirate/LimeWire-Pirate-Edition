package org.limewire.bittorrent;

public interface TorrentStatus {

    /**
     * @return the rate in bytes/second that data is being downloaded for this
     *          torrent. This includes payload and protocol overhead.
     */
    public float getDownloadRate();

    /**
     * @return the rate in byte/second that data is being uploaded for this
     *          torrent. This includes payload and protocol overhead.
     */
    public float getUploadRate();

    /**
     * @return the rate in bytes/second that payload data is being downloaded
     *          for this torrent.
     */
    public float getDownloadPayloadRate();

    /**
     * @return the rate in byte/second that payload data is being uploaded for
     *          this torrent.
     */
    public float getUploadPayloadRate();

    /**
     * @return the number of peers for this torrent.
     */
    public int getNumPeers();

    /**
     * @return the number of unchoked peers for this torrent.
     */
    public int getNumUploads();

    /**
     * @return the number of peers for this torrent are active seeds.
     */
    public int getNumSeeds();

    /**
     * @return the total number of open connections for this torrent.
     */
    public int getNumConnections();

    /**
     * @return the progress for downloading this torrent. A number from 0.0 to
     *          1.0 representing 0 to 100%.
     */
    public float getProgress();

    /**
     * @return the total amount of the torrent downloaded and verified.
     */
    public long getTotalDone();

    /**
     * @return the total amount of the torrent downloaded.
     */
    public long getAllTimePayloadDownload();

    /**
     * @return the total amount of the torrent uploaded.
     */
    public long getAllTimePayloadUpload();

    /**
     * @return the number of bytes of download that have been discarded
     *          due to error.
     */
    public long getTotalFailedDownload();
    
    /**
     * @return true if the torrent is paused.
     */
    public boolean isPaused();

    /**
     * @return true if the torrent is finished.
     */
    public boolean isFinished();

    /**
     * @return true if the torrent is in an error state.
     */
    public boolean isError();

    /**
     * @return the LibTorrentState for this torrent.
     */
    public TorrentState getState();

    /**
     * @return the seed ratio for this torrent.
     */
    public float getSeedRatio();

    /**
     * @return true if this torrent is automanaged.
     */
    public boolean isAutoManaged();

    /**
     * @return the amount of time the torrent has been seeding in seconds.
     */
    public int getSeedingTime();

    /**
     * @return the amount of time the torrent has been active in seconds.
     */
    public int getActiveTime();

    /**
     * @return the total number of bytes wanted to download. Some files in the
     *          torrent might have been marked as not to download. Those files bytes will
     *          not be included in this number.
     */
    public long getTotalWanted();

    /**
     * @return the total number of bytes wanted that have been downloaded. Some
     *          files in the torrent might have been marked as not to download. Those
     *          files bytes will not be included in this number.
     */
    public long getTotalWantedDone();

    /**
     * @return the internal error message for the torrent. If the torrent is in
     *          an Error state.
     */
    public String getError();

    /**
     * @return the current tracker for this torrent, null if no
     *          tracker was ever contacted.
     */
    public String getCurrentTracker();

    /**
     * @return Total number of peers that are seeding (complete).  
     *          -1 if no data from tracker
     */
    public int getNumComplete();
    
    /**
     * @return Total number of peers that are downloading (incomplete).
     *          -1 if no data from tracker
     */
    public int getNumIncomplete();
}