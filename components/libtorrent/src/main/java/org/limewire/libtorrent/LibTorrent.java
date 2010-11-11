package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentStatus;
import org.limewire.libtorrent.callback.AlertCallback;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;

/**
 * Interface definition for accessing the C LibTorrentWrapper library.
 */
interface LibTorrent extends Library {

    /**
     * Inititalizes the libtorrent session to use the given path as the default
     * download location.
     */
    public WrapperStatus init(LibTorrentSettings libTorrentSettings);

    /**
     * Adds a torrent to the libtorrent session. This can be done with only a
     * sha1 and trackerURI. optionally a path to a fast Resume data file can be
     * included to enable starting the torrent faster.
     */
    public WrapperStatus add_torrent(String sha1, String trackerURI, WString torrentPath,
            WString savePath, WString fastResumeData);

    /**
     * Pauses the torrent with the given sha1
     */
    public WrapperStatus pause_torrent(String id);

    /**
     * Resumes the torrent with the given sha1
     */
    public WrapperStatus resume_torrent(String id);

    /**
     * Forces the torrent to reannounce itself to the tracker.
     */
    public WrapperStatus force_reannounce(String id);

    /**
     * Scrapes the tracker to get updated torrent statistics.
     */
    public WrapperStatus scrape_tracker(String id);

    /**
     * Used on shutdown to freeze all torrents and wait while saving fast resume
     * data for each.
     */
    public WrapperStatus freeze_and_save_all_fast_resume_data(AlertCallback alertCallback);

    /**
     * Reads any stored alerts in the session, having there data coming in
     * through the callback.
     */
    public WrapperStatus get_alerts(AlertCallback alertCallback);

    /**
     * Set a callback for the JNA to call back into the java code in order to
     * filter out bad IP addresses of peers
     */
    public WrapperStatus set_ip_filter(IpFilterCallback ipFilterCallback);

    /**
     * Fills in the Libtorrent status struct for the torrent with the given sha1
     */
    public WrapperStatus get_torrent_status(String id, TorrentStatus status);

    /**
     * Returns the number of peers for the torrent with the given sha1
     */
    public WrapperStatus get_num_peers(String id, IntByReference numPeers);

    /**
     * Retrieves the peers for the torrent with the given sha1
     */
    public WrapperStatus get_peers(String id, Pointer[] torrentPeers, int numPeers);

    /**
     * Tells the session to save the fast resume data for the torrent with the
     * given sha1.
     */
    public WrapperStatus signal_fast_resume_data_request(String id);

    /**
     * Clears the error status on a torrent and attempts to restart it.
     */
    public WrapperStatus clear_error_and_retry(String id);

    /**
     * Removes the torrent with the given sha1 from the session.
     */
    public WrapperStatus remove_torrent(String id);

    /**
     * Moves the torrent with the given sha1 from its current location to the
     * location defined in absolutePath.
     */
    public WrapperStatus move_torrent(String id, WString absolutePath);

    /**
     * Aborts all of the torrents in the session.
     */
    public WrapperStatus abort_torrents();

    /**
     * Frees the given torrentStatus object from memory.
     */
    public WrapperStatus free_torrent_status(Pointer ptr);

    /**
     * Updates the sessions settings using the provided settings structure.
     */
    public WrapperStatus update_settings(LibTorrentSettings libTorrentSettings);

    /**
     * Starts the dht
     */
    public WrapperStatus start_dht(WString dht_state_file_path);

    /**
     * Stops the dht
     */
    public WrapperStatus stop_dht();

    /**
     * Adds the given node to the dht.
     */
    public WrapperStatus add_dht_node(String address, int port);

    /**
     * Adds the given router to the dht.
     */
    public WrapperStatus add_dht_router(String address, int port);

    /**
     * Saves the state of the dht in the specified file.
     */
    public WrapperStatus save_dht_state(WString dhtStateFilePath);

    /**
     * Starts the upnp service.
     */
    public WrapperStatus start_upnp();

    /**
     * Stops the upnp service.
     */
    public WrapperStatus stop_upnp();

    /**
     * Starts the local service discovery service
     */
    public WrapperStatus start_lsd();

    /**
     * Stops the local service discovery service
     */
    public WrapperStatus stop_lsd();

    /**
     * Starts the natpmp service.
     */

    public WrapperStatus start_natpmp();

    /**
     * Stops the natpmp service.
     */
    public WrapperStatus stop_natpmp();

    /**
     * Frees the peers from memory.
     */
    public WrapperStatus free_peers(Pointer[] torrentPeersPointers, int length);

    /**
     * Set the target seed ratio for this torrent.
     */
    public WrapperStatus set_seed_ratio(String id, float seed_ratio);
    

    /**
     * Sets the upload limit on this Torrent. If this limit is higher than the
     * global upload limit, it will not go higher than that limit.
     */
    public WrapperStatus set_upload_limit(String id, int limit);
    
    /**
     * Returns the upload limit for this Torrent. If the global upload limit is
     * being used, this will return 0.
     */
    public WrapperStatus get_upload_limit(String id, IntByReference limit);
    
    /**
     * Sets the download limit on this Torrent. If this limit is higher than the
     * global download limit, it will not go higher than that limit.
     */
    public WrapperStatus set_download_limit(String id, int limit);
    
    /**
     * Returns the download limit for this Torrent. If the global download limit
     * is being used, this will return 0.
     */
    public WrapperStatus get_download_limit(String id, IntByReference limit);

    /**
     * Sets the file priorities for the given torrent.
     */
    public WrapperStatus set_file_priorities(String id, int[] priorities, int num_priorities);

    /**
     * Returns the number of files for the given torrent.
     */
    public WrapperStatus get_num_files(String id, IntByReference numFiles);

    /**
     * Returns the files for the given torrent.
     */
    public WrapperStatus get_files(String id, Pointer[] filePointers);

    /**
     * Marks this torrent as automanged or not.
     */
    public WrapperStatus set_auto_managed_torrent(String sha1, boolean auto_managed);

    /**
     * This is a simple function used to test that the loaded library is
     * working.
     */
    public void echo(String message);

    /**
     * Sets the file priority for the given torrent and torrent file index to
     * the given priority.
     */
    public WrapperStatus set_file_priority(String sha1, int index, int priority);

    /**
     * Checks if the given torrent has metadata yet.
     */
    public WrapperStatus has_metadata(String sha1, IntByReference has_metadata);

    /**
     * Checks if the given torrent is Valid.
     */
    public WrapperStatus is_valid(String sha1, IntByReference is_valid);

    /**
     * Returns the TorrentInfo for the given torrent.
     */
    public WrapperStatus get_torrent_info(String sha1, LibTorrentInfo torrentInfo);

    /**
     * Frees memory for the torrent info.
     */
    public WrapperStatus free_torrent_info(Pointer pointer);

    /**
     * Saves the fast resume data for the given alert.
     */
    public WrapperStatus save_fast_resume_data(LibTorrentAlert alert, WString filePath);

    /**
     * Sets the peer proxy for the peer connections.
     */
    public WrapperStatus set_peer_proxy(LibTorrentProxySetting proxySetting);

    /**
     * Sets the dht proxy for dht connections.
     */
    public WrapperStatus set_dht_proxy(LibTorrentProxySetting proxySetting);

    /**
     * Sets the webseed proxy for the webseed connections.
     */
    public WrapperStatus set_web_seed_proxy(LibTorrentProxySetting proxySetting);

    /**
     * Sets the tracker proxy for tracker connections.
     */
    public WrapperStatus set_tracker_proxy(LibTorrentProxySetting proxySetting);
    
    /**
	 * Passes back the states of each piece in the torrent.
	 */
    public WrapperStatus get_pieces_status(String sha1, LibTorrentPiecesInfoContainer info);
    
    /**
	 * Frees the memory consumed by the pieces data memory allocated by
     * {@link #get_pieces_status()}
	 */
    public WrapperStatus free_pieces_info(Pointer pointer);
    
    /**
     * Adds a tracker to a torrent at a given tier.
     */
    public WrapperStatus add_tracker(String sha1, String url, int tier);
    
    /**
     * Removes any tracker with the matching name and tier. 
     */
    public WrapperStatus remove_tracker(String sha1, String url, int tier);
   
    /**
     * Gets the current number of trackers being used by a torrent.
     */
    public WrapperStatus get_num_trackers(String sha1, IntByReference numTrackers);
    
    
    /**
     * Gets the current list of trackers being used by a torrent.
     */
    public WrapperStatus get_trackers(String sha1, Pointer[] trackers, int numTrackers);
    
    /**
     * Frees the data consumed for tracker data.
     */
    public WrapperStatus free_trackers(Pointer[] trackers, int numTrackers);
    
//    public WrapperStatus queue_tracker_scrape_request(String sha1, String trackerUri, TrackerScrapeRequestCallback callback);
}
