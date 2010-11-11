package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.util.StringUtils;

import com.sun.jna.Structure;

/**
 * Structure used to pass data from the native code regarding a torrents state
 * back to java.
 */
public class LibTorrentStatus extends Structure implements TorrentStatus {

    /**
     * long containing the total amount of the torrent downloaded and verified.
     */
    public long total_done;

    /**
     * String containing the total amount of wanted bytes of the torrent
     * downloaded and verified.
     */
    public long total_wanted_done;

    /**
     * long containing the total amount of wanted bytes of the torrent.
     */
    public long total_wanted;

    /**
     * long containing the total amount of the torrent downloaded, this session.
     */
    public long total_download;

    /**
     * long containing the total amount of the torrent uploaded, this session.
     */
    public long total_upload;

    /**
     * long containing the total amount of the torrent payload downloaded, this
     * session.
     */
    public long total_payload_download;

    /**
     * long containing the total amount of the torrent payload uploaded, this
     * session.
     */
    public long total_payload_upload;

    /**
     * long containing the total amount of the torrent payload downloaded over
     * all time.
     */
    public long all_time_payload_download;

    /**
     * long containing the total amount of the torrent payload uploaded over all
     * time.
     */
    public long all_time_payload_upload;

    /**
     * The current rate of the torrent download in bytes/second
     */
    public float download_rate;

    /**
     * The current rate of the torrent upload in bytes/second
     */
    public float upload_rate;

    /**
     * The current rate of the torrent payload download in bytes/second
     */
    public float download_payload_rate;

    /**
     * The current rate of the torrent payload upload in bytes/second
     */
    public float upload_payload_rate;

    /**
     * The number of peers this torrent has.
     */
    public int num_peers;

    /**
     * The number of uplaod peers this torrent has.
     */
    public int num_uploads;

    /**
     * The number of seeds this torrent has.
     */
    public int num_seeds;

    /**
     * The number of connections this torrent has.
     */
    public int num_connections;

    /**
     * The state of this torrent, aligning with LibTorrentState.
     */
    public int state;

    /**
     * The progress of this torrents download, from 0 to 1.0.
     */
    public float progress;

    /**
     * boolean of whether this torrent is paused.
     */
    public int paused;

    /**
     * boolean of whether this torrent is finished.
     */
    public int finished;

    /**
     * boolean of whether this torrent is valid.
     */
    public int valid;

    /**
     * boolean of whether this torrent is auto managed.
     */
    public int auto_managed;

    /**
     * int for the amount of time this torrent has been seeding in seconds.
     */
    public int seeding_time;

    /**
     * int for the amount of time this torrent has been seeding in seconds.
     */
    public int active_time;

    /**
     * String containing the error message for the torrent. Null/empty if there
     * is no error.
     */
    public String error;

    /**
     * Represents the current tracker for this torrent. Null/empty if there was
     * never a successful tracker connect.
     */
    public String current_tracker;

    /**
     * Total number of peers that are seeding (complete).
     * <p> -1 if no data from tracker
     */
    public int num_complete;


    /**
     * Total number of peers that are downloading (incomplete).
     * <p> -1 if no data from tracker
     */
    public int num_incomplete;
    
    /**
     * Amount of download data that has been discarded due to error.
     */
    public long total_failed_bytes;
    
    @Override
    public float getDownloadPayloadRate() {
        return download_payload_rate;
    }

    @Override
    public float getUploadPayloadRate() {
        return upload_payload_rate;
    }

    @Override
    public int getNumPeers() {
        return num_peers;
    }

    @Override
    public int getNumUploads() {
        return num_uploads;
    }

    @Override
    public int getNumSeeds() {
        return num_seeds;
    }

    @Override
    public int getNumConnections() {
        return num_connections;
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public long getTotalDone() {
        return total_done;
    }

    @Override
    public long getAllTimePayloadDownload() {
        return all_time_payload_download;
    }

    @Override
    public long getAllTimePayloadUpload() {
        return all_time_payload_upload;
    }

    @Override
    public long getTotalFailedDownload() {
        return total_failed_bytes;
    }
    
    @Override
    public boolean isPaused() {
        return paused != 0;
    }

    @Override
    public boolean isFinished() {
        return finished != 0;
    }

    @Override
    public boolean isError() {
        return !StringUtils.isEmpty(error);
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public TorrentState getState() {
        LibTorrentState libTorrentState = LibTorrentState.forId(state);
        switch (libTorrentState) {
        case ALLOCATING:
            return TorrentState.ALLOCATING;
        case CHECKING_FILES:
            return TorrentState.CHECKING_FILES;
        case DOWNLOADING:
            return TorrentState.DOWNLOADING;
        case DOWNLOADING_METADATA:
            return TorrentState.DOWNLOADING_METADATA;
        case FINISHED:
            return TorrentState.FINISHED;
        case QUEUED_FOR_CHECKING:
            return TorrentState.QUEUED_FOR_CHECKING;
        case SEEDING:
            return TorrentState.SEEDING;
        default:
            throw new UnsupportedOperationException("No known state for id: " + state
                    + " and libtorrent state: " + libTorrentState);
        }
    }

    @Override
    public float getSeedRatio() {
        if (getAllTimePayloadDownload() <= 0 || getAllTimePayloadUpload() <= 0) {
            return 0;
        }
        float seedRatio = getAllTimePayloadUpload() / (float) getAllTimePayloadDownload();
        return seedRatio;
    }

    @Override
    public float getDownloadRate() {
        return download_rate;
    }

    @Override
    public float getUploadRate() {
        return upload_rate;
    }

    @Override
    public boolean isAutoManaged() {
        return false;
    }

    @Override
    public int getSeedingTime() {
        // Partial downloads have no notion of seeding time since they never
        // fully
        // enter the seeding state. Instead use the active time in this case for
        // comparison.
        // TODO: Update when something better becomes available. This will cause
        // partial torrent download seeds to become stale comparatively sooner
        // than their normal peers.
        //TODO after talking with arvid, he will add a finished_time variable for this extraneous case.
        //it will be a part of 0.15.0 when it is released.
        if (seeding_time == 0 && LibTorrentState.forId(state) == LibTorrentState.FINISHED) {
            return active_time;
        } else {
            return seeding_time;
        }
    }

    @Override
    public int getActiveTime() {
        return active_time;
    }

    @Override
    public long getTotalWanted() {
        return total_wanted;
    }

    @Override
    public long getTotalWantedDone() {
        return total_wanted_done;
    }

    @Override
    public String getCurrentTracker() {
        return current_tracker != null && current_tracker.isEmpty() ? null : current_tracker;
    }

    @Override
    public int getNumComplete() {
        return num_complete;
    }

    @Override
    public int getNumIncomplete() {
        return num_incomplete;
    }
}
