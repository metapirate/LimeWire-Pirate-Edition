package org.limewire.libtorrent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.bittorrent.TorrentException;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentPiecesInfo;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.inject.LazySingleton;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.OSUtils;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;

/**
 * Wrapper class for the LibTorrent c interface. Provides library loading logic,
 * and handles rethrowing c++ exceptions as java exceptions.
 */
@LazySingleton
class LibTorrentWrapper {

    private static final Log LOG = LogFactory.getLog(LibTorrentWrapper.class);

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private LibTorrent libTorrent;

    /**
     * Initializes the LibTorrent library. Finding necessary dependencies first,
     * then loading the libtorrent library as a jna lib.
     */
    void initialize(TorrentManagerSettings torrentSettings) {
        
        String torrentWrapperLibName = getLibraryName();
        
        try {
            this.libTorrent = (LibTorrent) Native.loadLibrary(torrentWrapperLibName, LibTorrent.class);
            NativeLibrary lib = NativeLibrary.getInstance(torrentWrapperLibName);
            validate(lib);
            init(torrentSettings);
            // TODO add get_version method to the wrapper that can be checked
            // here as well.
            loaded.set(true);
        } catch (Throwable e) {
            LOG.error("Failure loading the libtorrent libraries.", e);
            if (torrentSettings.isReportingLibraryLoadFailture()) {
                ExceptionUtils.reportOrReturn(e);
            }
        }
    }

    /**
     * @return the name of the shared library containing the libtorrentwrapper code.
     *          Should match the file name of the library on disk. 
     */
    private static String getLibraryName() {
        if (OSUtils.isLinux()) {
            String arch = OSUtils.getOSArch();
            // Linux requires binaries for every arch.
            if (arch.equals("x86_64") || arch.equals("amd64")) {
                return "torrent-wrapper64";
            } 
        } 
        return "torrent-wrapper";
    }
    
    /**
     * Validates that all of the expected functions exist in the library.
     */
    private void validate(NativeLibrary lib) {
        for (Method method : LibTorrent.class.getMethods()) {
            // throws exception if there is an error finding the function
            lib.getFunction(method.getName());
        }
    }

    /**
     * Returns true if the native library was found and was able to load
     * successfully.
     */
    public boolean isLoaded() {
        return loaded.get();
    }

    private void init(TorrentManagerSettings torrentSettings) {
        LOG.debugf("before init");
        catchWrapperException(libTorrent.init(new LibTorrentSettings(torrentSettings)));
        LOG.debugf("after init");
    }

    public void add_torrent(String sha1, String trackerURI, String torrentPath, String savePath,
            String fastResumePath) {
        LOG.debugf("before add_torrent: {0}", sha1);
        catchWrapperException(libTorrent.add_torrent(sha1, trackerURI, 
                torrentPath != null ? new WString(torrentPath) : null,
                new WString(savePath),
                new WString(fastResumePath)));
        LOG.debugf("after add_torrent: {0}", sha1);
    }

    public void freeze_and_save_all_fast_resume_data(AlertCallback alertCallback) {
        LOG.debug("before get_alerts");
        catchWrapperException(libTorrent.freeze_and_save_all_fast_resume_data(alertCallback));
        LOG.debug("after get_alerts");
    }

    public void get_alerts(AlertCallback alertCallback) {
        LOG.debug("before get_alerts");
        catchWrapperException(libTorrent.get_alerts(alertCallback));
        LOG.debug("after get_alerts");
    }

    public void set_ip_filter(IpFilterCallback ipFilterCallback) {
        LOG.debug("before set_ip_filter");
        catchWrapperException(libTorrent.set_ip_filter(ipFilterCallback));
        LOG.debug("after set_ip_filter");
    }

    public void pause_torrent(String id) {
        LOG.debugf("before pause_torrent: {0}", id);
        catchWrapperException(libTorrent.pause_torrent(id));
        LOG.debugf("after pause_torrent: {0}", id);
    }

    public void resume_torrent(String id) {
        LOG.debugf("before resume_torrent: {0}", id);
        catchWrapperException(libTorrent.resume_torrent(id));
        LOG.debugf("after resume_torrent: {0}", id);
    }

    public void scrape_tracker(String id) {
        LOG.debugf("before scrape_tracker: {0}", id);
        catchWrapperException(libTorrent.scrape_tracker(id));
        LOG.debugf("after scrape_tracker: {0}", id);
    }

    public void force_reannounce(String id) {
        LOG.debugf("before force_reannounce: {0}", id);
        catchWrapperException(libTorrent.force_reannounce(id));
        LOG.debugf("after force_reannounce: {0}", id);
    }

    public void get_torrent_status(String id, TorrentStatus status) {
        LOG.debugf("before get_torrent_status: {0}", id);
        catchWrapperException(libTorrent.get_torrent_status(id, status));
        LOG.debugf("after get_torrent_status: {0}", id);
    }

    public void remove_torrent(String id) {
        LOG.debugf("before remove_torrent: {0}", id);
        catchWrapperException(libTorrent.remove_torrent(id));
        LOG.debugf("after remove_torrent: {0}", id);
    }

    public LibTorrentPeer[] get_peers(String id) {

        LOG.debugf("before get_num_peers: {0}", id);
        IntByReference numPeersReference = new IntByReference();
        catchWrapperException(libTorrent.get_num_peers(id, numPeersReference));
        LOG.debugf("after get_num_peers: {0} - {1}", id, numPeersReference);

        int numPeers = numPeersReference.getValue();

        if (numPeers == 0) {
            return new LibTorrentPeer[0];
        }

        LibTorrentPeer[] torrentPeers = new LibTorrentPeer[numPeers];
        Pointer[] torrentPeersPointers = new Pointer[numPeers];
        for (int i = 0; i < torrentPeersPointers.length; i++) {
            LibTorrentPeer torrentPeer = new LibTorrentPeer();
            torrentPeers[i] = torrentPeer;
            torrentPeersPointers[i] = torrentPeer.getPointer();
        }

        LOG.debugf("before get_peers: {0}", id);
        catchWrapperException(libTorrent.get_peers(id, torrentPeersPointers,
                torrentPeersPointers.length));

        // Trim any empty peers if the list was not filled
        List<LibTorrentPeer> remainingTorrentPeers = new ArrayList<LibTorrentPeer>(torrentPeers.length);

        for (int i = 0; i < torrentPeers.length; i++) {
            torrentPeers[i].read();
            if (torrentPeers[i].getIPAddress() != null && !torrentPeers[i].getIPAddress().isEmpty()) {
                remainingTorrentPeers.add(torrentPeers[i]);
            }
        }
     
        LOG.debugf("before free_peers: {0}", id);
        free_peers(torrentPeersPointers);
        LOG.debugf("after free_peers: {0}", id);
        
        LOG.debugf("after get_peers: {0}", id);
        
        return remainingTorrentPeers.toArray(new LibTorrentPeer[remainingTorrentPeers.size()]);
    }

    private void free_peers(Pointer[] torrentPeersPointers) {
        catchWrapperException(libTorrent.free_peers(torrentPeersPointers,
                torrentPeersPointers.length));
    }

    public void signal_fast_resume_data_request(String id) {
        LOG.debugf("before print signal_fast_resume_data_request: {0}", id);
        catchWrapperException(libTorrent.signal_fast_resume_data_request(id));
        LOG.debugf("after print signal_fast_resume_data_request: {0}", id);
    }

    public void clear_error_and_retry(String id) {
        LOG.debugf("before print clear_error_and_retry: {0}", id);
        catchWrapperException(libTorrent.clear_error_and_retry(id));
        LOG.debugf("after print clear_error_and_retry: {0}", id);
    }

    public void move_torrent(String id, String absolutePath) {
        LOG.debugf("before move_torrent: {0} - {1}", id, absolutePath);
        catchWrapperException(libTorrent.move_torrent(id, new WString(absolutePath)));
        LOG.debugf("after move_torrent: {0} - {1}", id, absolutePath);
    }

    public void abort_torrents() {
        LOG.debug("before abort");
        catchWrapperException(libTorrent.abort_torrents());
        LOG.debug("after abort");
    }

    public void free_torrent_status(LibTorrentStatus status) {
        LOG.debugf("before free_torrent_status: {0}", status);
        catchWrapperException(libTorrent.free_torrent_status(status.getPointer()));
        LOG.debugf("after free_torrent_status: {0}", status);
    }

    private void catchWrapperException(WrapperStatus status) {
        if (status != null) {
            throw new TorrentException(status.message, status.type);
        }
    }

    public void update_settings(TorrentManagerSettings torrentSettings) {
        LOG.debugf("before update_settings: {0}", torrentSettings);
        catchWrapperException(libTorrent.update_settings(new LibTorrentSettings(torrentSettings)));
        LOG.debugf("after update_settings: {0}", torrentSettings);
    }

    public void start_dht() {
        start_dht(null);
    }

    public void start_dht(File dhtStateFile) {
        LOG.debugf("before start_dht");
        WString dhtStateFilePath = dhtStateFile != null ? new WString(dhtStateFile
                .getAbsolutePath()) : null;
        catchWrapperException(libTorrent.start_dht(dhtStateFilePath));
        LOG.debugf("after start_dht");
    }

    public void stop_dht() {
        LOG.debugf("before stop_dht");
        catchWrapperException(libTorrent.stop_dht());
        LOG.debugf("after stop_dht");
    }

    public void save_dht_state(File dhtStateFile) {
        LOG.debugf("before save_dht_state");
        if (dhtStateFile != null) {
            dhtStateFile.getParentFile().mkdirs();
        }
        WString dhtStateFilePath = dhtStateFile != null ? new WString(dhtStateFile
                .getAbsolutePath()) : null;
        catchWrapperException(libTorrent.save_dht_state(dhtStateFilePath));
        LOG.debugf("after save_dht_state");
    }

    public void add_dht_router(String address, int port) {
        LOG.debugf("before add_dht_router: address={0}, port={1}", address, port);
        catchWrapperException(libTorrent.add_dht_router(address, port));
        LOG.debugf("after add_dht_router: address={0}, port={1}", address, port);
    }

    public void add_dht_node(String address, int port) {
        LOG.debugf("before add_dht_node: address={0}, port={1}", address, port);
        catchWrapperException(libTorrent.add_dht_node(address, port));
        LOG.debugf("after add_dht_node: address={0}, port={1}", address, port);
    }

    public void start_upnp() {
        LOG.debugf("before start_upnp");
        catchWrapperException(libTorrent.start_upnp());
        LOG.debugf("after start_upnp");
    }

    public void stop_upnp() {
        LOG.debugf("before stop_upnp");
        catchWrapperException(libTorrent.stop_upnp());
        LOG.debugf("after stop_upnp");
    }

    public void start_lsd() {
        LOG.debugf("before start_lsd");
        catchWrapperException(libTorrent.start_lsd());
        LOG.debugf("after start_lsd");
    }

    public void stop_lsd() {
        LOG.debugf("before stop_lsd");
        catchWrapperException(libTorrent.stop_lsd());
        LOG.debugf("after stop_lsd");
    }

    public void start_natpmp() {
        LOG.debugf("before start_natpmp");
        catchWrapperException(libTorrent.start_natpmp());
        LOG.debugf("after start_natpmp");
    }

    public void stop_natpmp() {
        LOG.debugf("before stop_natpmp");
        catchWrapperException(libTorrent.stop_natpmp());
        LOG.debugf("after stop_natpmp");
    }

    /**
     * Set the target seed ratio for this torrent.
     */
    // TODO: disable until talking to everrettt
    public void set_seed_ratio(String id, float seed_ratio) {
        LOG.debugf("before set_seed_ratio: {0} - {1}", id, seed_ratio);
        catchWrapperException(libTorrent.set_seed_ratio(id, seed_ratio));
        LOG.debugf("after set_seed_ratio");
    }

    /**
     * Sets the upload limit for this particular Torrent.
     */
    public void set_upload_limit(String id, int limit) {
        LOG.debugf("before set_upload_limit: {0} - {1}", id, limit);
        catchWrapperException(libTorrent.set_upload_limit(id, limit));
        LOG.debugf("after set_upload_limit");
    }
    
    /**
     * Returns the upload limit for this particular Torrent. If
     * the global upload limit is being used, this will return 0.
     */
    public int get_upload_limit(String id) {
        LOG.debugf("before get_upload_limit");
        IntByReference limit = new IntByReference();
        catchWrapperException(libTorrent.get_upload_limit(id, limit));
        LOG.debugf("after get_upload_limit");
        return limit.getValue();
    }
    
    /**
     * Sets the download limit for this particular Torrent.
     */
    public void set_download_limit(String id, int limit) {
        LOG.debugf("before set_download_limit");
        catchWrapperException(libTorrent.set_download_limit(id, limit));
        LOG.debugf("after set_download_limit");
    }
    
    /**
     * Returns the download limit for this particular Torrent. If
     * the global download limit is being used, this will return 0.
     */
    public int get_download_limit(String id) {
        LOG.debugf("before get_download_limit");
        IntByReference limit = new IntByReference();
        catchWrapperException(libTorrent.get_download_limit(id, limit));
        LOG.debugf("after get_download_limit");
        return limit.getValue();
    }
    
    /**
     * Sets the file priority for the given index.
     */
    public void set_file_priorities(String id, int[] priorities) {
        LOG.debugf("before set_file_priorities");
        catchWrapperException(libTorrent.set_file_priorities(id, priorities, priorities.length));
        LOG.debugf("after set_file_priorities");
    }

    /**
     * Returns the number of files for the given torrent.
     */
    public int get_num_files(String id) {
        LOG.debugf("before get_num_files");
        IntByReference numFiles = new IntByReference();
        catchWrapperException(libTorrent.get_num_files(id, numFiles));
        LOG.debugf("after get_num_files");
        return numFiles.getValue();
    }

    /**
     * Returns the files for the given torrent.
     */
    public LibTorrentFileEntry[] get_files(String id) {
        LOG.debugf("before get_files");
        int numFiles = get_num_files(id);
        LibTorrentFileEntry[] fileEntries = new LibTorrentFileEntry[numFiles];
        Pointer[] filePointers = new Pointer[numFiles];
        for (int i = 0; i < fileEntries.length; i++) {
            LibTorrentFileEntry fileEntry = new LibTorrentFileEntry();
            fileEntries[i] = fileEntry;
            filePointers[i] = fileEntry.getPointer();
        }

        catchWrapperException(libTorrent.get_files(id, filePointers));

        for (int i = 0; i < fileEntries.length; i++) {
            fileEntries[i].read();
        }

        LOG.debugf("after get_files");
        return fileEntries;
    }

    public void set_auto_managed_torrent(String sha1, boolean auto_managed) {
        LOG.debugf("before set_auto_managed_torrent: {0} - {1}", sha1, auto_managed);
        catchWrapperException(libTorrent.set_auto_managed_torrent(sha1, auto_managed));
        LOG.debugf("after set_auto_managed_torrent: {0} - {1}", sha1, auto_managed);
    }

    public void set_file_priority(String sha1, int index, int priority) {
        LOG.debugf("before set_file_priority: {0} - index: {1} - priority: {2}", sha1, index,
                priority);
        catchWrapperException(libTorrent.set_file_priority(sha1, index, priority));
        LOG.debugf("after set_file_priority: {0} - index: {1} - priority: {2}", sha1, index,
                priority);
    }

    public boolean has_metadata(String id) {
        LOG.debugf("before has_metadata: {0}", id);
        IntByReference has_metadata = new IntByReference(0);
        catchWrapperException(libTorrent.has_metadata(id, has_metadata));
        LOG.debugf("after has_metadata: {0}", id);
        return has_metadata.getValue() != 0;
    }

    public boolean is_valid(String id) {
        LOG.debugf("before is_valid: {0}", id);
        IntByReference is_valid = new IntByReference(0);
        catchWrapperException(libTorrent.is_valid(id, is_valid));
        LOG.debugf("after is_valid: {0}", id);
        return is_valid.getValue() != 0;
    }

    public TorrentInfo get_torrent_info(String id) {
        LOG.debugf("before get_torrent_info: {0}", id);
        LibTorrentInfo info = new LibTorrentInfo();
        catchWrapperException(libTorrent.get_torrent_info(id, info));
        free_torrent_info(info);
        TorrentFileEntry[] files = get_files(id);
        LOG.debugf("after get_torrent_info: {0}", id);
        return new TorrentInfoImpl(info, files);
    }

    public void free_torrent_info(LibTorrentInfo info) {
        LOG.debugf("before free_torrent_info: {0}", info);
        catchWrapperException(libTorrent.free_torrent_info(info.getPointer()));
        LOG.debugf("after free_torrent_info: {0}", info);
    }

    /**
     * Saves the fast resume data for the given alert.
     */
    public void save_fast_resume_data(LibTorrentAlert alert, String filePath) {
        LOG.debugf("before save_fast_resume_data: {0} - {1}", alert, filePath);
        catchWrapperException(libTorrent.save_fast_resume_data(alert, new WString(filePath)));
        LOG.debugf("after save_fast_resume_data: {0} - {1}", alert, filePath);
    }

    public void set_peer_proxy(LibTorrentProxySetting proxySetting) {
        LOG.debugf("before set_peer_proxy: {0}", proxySetting);
        catchWrapperException(libTorrent.set_peer_proxy(proxySetting));
        LOG.debugf("after set_peer_proxy: {0}", proxySetting);
    }

    public void set_dht_proxy(LibTorrentProxySetting proxySetting) {
        LOG.debugf("before set_dht_proxy: {0}", proxySetting);
        catchWrapperException(libTorrent.set_dht_proxy(proxySetting));
        LOG.debugf("after set_dht_proxy: {0}", proxySetting);
    }

    public void set_tracker_proxy(LibTorrentProxySetting proxySetting) {
        LOG.debugf("before set_tracker_proxy: {0}", proxySetting);
        catchWrapperException(libTorrent.set_tracker_proxy(proxySetting));
        LOG.debugf("after set_tracker_proxy: {0}", proxySetting);
    }

    public void set_web_seed_proxy(LibTorrentProxySetting proxySetting) {
        LOG.debugf("before set_web_seed_proxy: {0}", proxySetting);
        catchWrapperException(libTorrent.set_web_seed_proxy(proxySetting));
        LOG.debugf("after set_web_seed_proxy: {0}", proxySetting);
    }
       
    public TorrentPiecesInfo get_pieces_status(String sha1) {
        
        LibTorrentPiecesInfoContainer info = new LibTorrentPiecesInfoContainer(); 
        
        LOG.debugf("before get_pieces_status: {0}", sha1);
        catchWrapperException(libTorrent.get_pieces_status(sha1, info));
        LOG.debugf("after get_pieces_status: {0}", sha1);
                
        TorrentPiecesInfo exportInfo = new LibTorrentPiecesInfo(info);
        
        LOG.debugf("before free_pieces_info: {0}", sha1);
        catchWrapperException(libTorrent.free_pieces_info(info.getPointer()));
        LOG.debugf("after free_pieces_info: {0}", sha1);
        
        return exportInfo;
    }
    
    public void add_tracker(String sha1, String url, int tier) {
        LOG.debugf("before add_tracker: {0}", sha1);
        catchWrapperException(libTorrent.add_tracker(sha1, url, tier));
        LOG.debugf("after add_tracker: {0}", sha1);
    }
    
    public void remove_tracker(String sha1, String url, int tier) {
        LOG.debugf("before remove_tracker: {0}", sha1);
        catchWrapperException(libTorrent.remove_tracker(sha1, url, tier));
        LOG.debugf("after remove_tracker: {0}", sha1);
    }
        
    public LibTorrentAnnounceEntry[] get_trackers(String sha1) {
        
        int numTrackers = get_num_trackers(sha1);
        
        if (numTrackers == 0) {
            return new LibTorrentAnnounceEntry[0];
        }
        
        LibTorrentAnnounceEntry[] torrentTrackers = new LibTorrentAnnounceEntry[numTrackers];
        Pointer[] torrentTrackersPointers = new Pointer[numTrackers];
        for ( int i=0 ; i < torrentTrackersPointers.length ; i++ ) {
            LibTorrentAnnounceEntry torrentTracker = new LibTorrentAnnounceEntry();
            torrentTrackers[i] = torrentTracker;
            torrentTrackersPointers[i] = torrentTracker.getPointer();
        }
        
        LOG.debugf("before get_trackers: {0}", sha1);
        catchWrapperException(libTorrent.get_trackers(sha1, torrentTrackersPointers, numTrackers));
        LOG.debugf("after get_trackers: {0}", sha1);
        
        for ( LibTorrentAnnounceEntry tracker : torrentTrackers ) {
            tracker.read();
        }
        
        LOG.debugf("before free_trackers: {0}", sha1);
        free_trackers(torrentTrackersPointers);
        LOG.debugf("after free_trackers: {0}", sha1);
        
        return torrentTrackers;
    }
    
    private int get_num_trackers(String sha1) {
        IntByReference numTrackersReference = new IntByReference();
        
        LOG.debugf("before get_num_trackers: {0}", sha1);
        catchWrapperException(libTorrent.get_num_trackers(sha1, numTrackersReference));
        LOG.debugf("after get_num_trackers: {0}", sha1);
        
        return numTrackersReference.getValue();
    }
        
    private void free_trackers(Pointer[] torrentTrackerPointers) {
        catchWrapperException(libTorrent.free_trackers(torrentTrackerPointers,
                torrentTrackerPointers.length));
    }

    public void queue_tracker_scrape_request(String sha1String, String trackerUri, TrackerScrapeRequestCallback callback) {
        //catchWrapperException(libTorrent.queue_tracker_scrape_request(sha1String, trackerUri, callback));
    }
}
