package org.limewire.libtorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.limewire.bittorrent.ProxySetting;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentException;
import org.limewire.bittorrent.TorrentIpFilter;
import org.limewire.bittorrent.TorrentIpPort;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;
import org.limewire.inject.LazySingleton;
import org.limewire.libtorrent.callback.AlertCallback;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
public class LibTorrentSession implements TorrentManager {

    private static final Log LOG = LogFactory.getLog(LibTorrentSession.class);

    private final ScheduledExecutorService fastExecutor;

    private final LibTorrentWrapper libTorrent;

    private final Map<String, Torrent> torrents;

    private final BasicAlertCallback alertCallback = new BasicAlertCallback();

    // We maintain a member variable in order to prevent the JVM from
    // garbage collecting something the C++ libtorrent code relies on.
    @SuppressWarnings("unused")
    private IpFilterCallback ipFilterCallback;

    private final AtomicReference<TorrentManagerSettings> torrentSettings = new AtomicReference<TorrentManagerSettings>(
            null);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean dhtStarted = new AtomicBoolean(false);

    private final AtomicBoolean upnpStarted = new AtomicBoolean(false);

    private final EventListener<TorrentEvent> torrentListener = new EventListener<TorrentEvent>() {
        @Override
        public void handleEvent(TorrentEvent event) {
            handleTorrentEvent(event);
        }
    };

    /**
     * Used to protect from calling libtorrent code with invalid torrent data.
     * Locks access around libtorrent and removing/updating the torrents map to
     * make sure the torrents torrent manger knows about are the same as what
     * libtorrent knows about.
     */
    private final Lock lock = new ReentrantLock();

    private final List<ScheduledFuture<?>> torrentManagerTasks;
    
    @Inject
    public LibTorrentSession(LibTorrentWrapper torrentWrapper,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.fastExecutor = fastExecutor;
        this.libTorrent = torrentWrapper;
        this.torrents = new HashMap<String, Torrent>();
        this.torrentSettings.set(torrentSettings);
        this.torrentManagerTasks = new ArrayList<ScheduledFuture<?>>();
    }

    private void validateLibrary() {
        if (!initialized.get()) {
            throw new TorrentException("The Torrent Manager must be initialized first.",
                    TorrentException.INITIALIZATION_EXCEPTION);
        }
        if (!torrentSettings.get().isTorrentsEnabled()) {
            throw new TorrentException("LibTorrent is disabled (through settings)",
                    TorrentException.DISABLED_EXCEPTION);
        }
        if (!isValid()) {
            throw new TorrentException("There was a problem loading LibTorrent",
                    TorrentException.LOAD_EXCEPTION);
        }
    }

    @Override
    public Torrent addTorrent(TorrentParams params) throws IOException {
        assert started.get();
        lock.lock();
        try {
            validateLibrary();
            File torrentFile = params.getTorrentFile();
            File fastResumefile = params.getFastResumeFile();

            List<URI> trackerList = params.getTrackers();
            String firstTrackerURI = null;
            if (trackerList != null && trackerList.size()>0) {
                firstTrackerURI = trackerList.get(0).toASCIIString();
            }
            
            String fastResumePath = fastResumefile != null ? fastResumefile.getAbsolutePath()
                    : null;
            
            String torrentPath = torrentFile != null ? torrentFile.getAbsolutePath() : null;
            String saveDirectory = params.getDownloadFolder().getAbsolutePath();
            
            String sha1 = params.getSha1();
            
            Torrent torrent = new TorrentImpl(params, libTorrent, fastExecutor);
            libTorrent.add_torrent(sha1, firstTrackerURI, torrentPath, saveDirectory,
                    fastResumePath);
            
            // Flush and add the SECONDARY i={1,..,n} trackers again so if there were user changes, ie. 
            //  when loading from a memento the new user tracker list will be used.
            if (trackerList != null) {
                LOG.debug("flushing trackers");
                
                LibTorrentAnnounceEntry[] trackers = libTorrent.get_trackers(sha1);
               
                // Remove the trackers that were automatically added by loading the torrent file
                for ( int i=1 ; i<trackers.length ; i++ ) {
                    LibTorrentAnnounceEntry tracker = trackers[i];
                    libTorrent.remove_tracker(sha1, tracker.uri, tracker.tier);
                }
                
                // Add back for changes
                for ( int i=1 ; i<trackerList.size() ; i++ ) {
                    URI trackerURI = trackerList.get(i);
                    if (trackerURI != null) {
                        libTorrent.add_tracker(sha1, trackerURI.toASCIIString(), i);
                    }
                }
            }
            
            updateStatus(torrent);
            torrents.put(sha1, torrent);
            torrent.addListener(torrentListener);
            return torrent;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isValid() {
        return libTorrent.isLoaded();
    }

    @Override
    public Lock getLock() {
        return lock;
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        validateLibrary();
        lock.lock();
        try {
            torrent.removeListener(torrentListener);
            torrents.remove(torrent.getSha1());
            libTorrent.remove_torrent(torrent.getSha1());
        } finally {
            lock.unlock();
        }
    }

    private LibTorrentStatus getStatus(Torrent torrent) {
        validateLibrary();
        LibTorrentStatus status = new LibTorrentStatus();
        String sha1 = torrent.getSha1();
        libTorrent.get_torrent_status(sha1, status);
        libTorrent.free_torrent_status(status);
        return status;
    }

    private void updateStatus(Torrent torrent) {
        lock.lock();
        try {
            LibTorrentStatus torrentStatus = getStatus(torrent);
            torrent.updateStatus(torrentStatus);
        } finally {
            lock.unlock();
        }
    }

    private void handleTorrentEvent(TorrentEvent event) {
        if (event.getType() == TorrentEventType.STOPPED) {
            removeTorrent(event.getTorrent());
        }
    }

    @Override
    public void initialize() {
        if (!initialized.getAndSet(true)) {
            if (torrentSettings.get().isTorrentsEnabled()) {
                lock.lock();
                try {
                    libTorrent.initialize(torrentSettings.get());
                    if (libTorrent.isLoaded()) {
                        setTorrentManagerSettings(torrentSettings.get());
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    @Override
    public void setIpFilter(TorrentIpFilter ipFilter) {
        lock.lock();
        try {
            IpFilterCallback ipFilterCallback = new IpFilterCallback(ipFilter);
            libTorrent.set_ip_filter(ipFilterCallback);
            this.ipFilterCallback = ipFilterCallback;
        } finally {
            lock.unlock();
        }
    }

    public void scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
            TimeUnit unit) {
        lock.lock();
        try {
            ScheduledFuture<?> scheduledFuture = fastExecutor.scheduleWithFixedDelay(command,
                    initialDelay, delay, unit);
            torrentManagerTasks.add(scheduledFuture);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void start() {
        assert !started.get();
        started.set(true);
        if (isValid()) {
            lock.lock();
            try {
                scheduleWithFixedDelay(new AlertPoller(), 1000, 500, TimeUnit.MILLISECONDS);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            for (ScheduledFuture<?> scheduledFuture : torrentManagerTasks) {
                scheduledFuture.cancel(true);
            }

            if (isValid()) {
                libTorrent.freeze_and_save_all_fast_resume_data(alertCallback);
                if (isDHTStarted()) {
                    libTorrent.save_dht_state(torrentSettings.get().getDHTStateFile());
                }
                libTorrent.abort_torrents();
            }
            torrents.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Torrent getTorrent(File torrentFile) {
        if (torrentFile != null) {
            lock.lock();
            try {
                for (Torrent torrent : torrents.values()) {
                    if (torrentFile.equals(torrent.getTorrentFile())) {
                        return torrent;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    @Override
    public Torrent getTorrent(String sha1) {
        lock.lock();
        try {
            return torrents.get(sha1);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Basic implementation of the AlertCallback interface used to delegate
     * alerts back to the appropriate torrent.
     */
    private class BasicAlertCallback implements AlertCallback {

        private final Set<String> updatedTorrents = new HashSet<String>();

        @Override
        public void callback(LibTorrentAlert alert) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(alert.toString());
            }

            String sha1 = alert.getSha1();
            if (sha1 != null) {
                updatedTorrents.add(sha1);

                if (alert.getCategory() == LibTorrentAlert.storage_notification) {
                    Torrent torrent = getTorrent(sha1);
                    if (torrent != null) {
                        libTorrent.save_fast_resume_data(alert, torrent.getFastResumeFile()
                                .getAbsolutePath());
                        torrent.handleFastResumeAlert(alert);
                    }
                } else if (alert.getCategory() == LibTorrentAlert.status_notification 
                        && alert.getMessage().indexOf("metadata successfully received") > -1) {
                    Torrent torrent = getTorrent(sha1);
                    if (torrent != null) {
                        // Force update of torrent info
                        torrent.getTorrentInfo();
                    }
                }
            }
        }

        /**
         * Updates the status of all torrents that have recently recieved an
         * event
         */
        public void updateAlertedTorrents() {
            for (String sha1 : updatedTorrents) {
                Torrent torrent = getTorrent(sha1);
                if (torrent != null) {
                    updateStatus(torrent);
                }
            }
            updatedTorrents.clear();
        }
    }

    /**
     * Used to clear the alert queue in the native code passing event back to
     * the java code through the alertCallback interface.
     */
    private class AlertPoller implements Runnable {

        @Override
        public void run() {
            // Handle any alerts for fastresume/progress/status changes
            libTorrent.get_alerts(alertCallback);

            // Update status of alerted torrents
            alertCallback.updateAlertedTorrents();
        }
    }

    @Override
    public boolean isDownloadingTorrent(File torrentFile) {
        if (torrentFile != null) {
            lock.lock();
            try {
                for (Torrent torrent : torrents.values()) {
                    if (torrentFile.equals(torrent.getTorrentFile()) && !torrent.isFinished()) {
                        return true;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    @Override
    public void setTorrentManagerSettings(TorrentManagerSettings settings) {
        validateLibrary();
        lock.lock();
        try {
            torrentSettings.set(settings);
            libTorrent.update_settings(settings);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TorrentManagerSettings getTorrentManagerSettings() {
        return torrentSettings.get();
    }

    @Override
    public boolean isInitialized() {
        return isValid();
    }

    @Override
    public List<Torrent> getTorrents() {
        lock.lock();
        try {
            return new ArrayList<Torrent>(this.torrents.values());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isDHTStarted() {
        return dhtStarted.get();
    }

    @Override
    public void startDHT(File dhtStateFile) {
        validateLibrary();
        lock.lock();
        try {
            libTorrent.start_dht(dhtStateFile);
            addDHTRouters();
            dhtStarted.set(true);
        } finally {
            lock.unlock();
        }
    }

    private void addDHTRouters() {
        for (TorrentIpPort ipPort : getTorrentManagerSettings().getBootStrapDHTRouters()) {
            libTorrent.add_dht_router(ipPort.getAddress(), ipPort.getPort());
        }
    }

    @Override
    public void stopDHT() {
        validateLibrary();
        lock.lock();
        try {
            libTorrent.stop_dht();
            dhtStarted.set(false);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void saveDHTState(File dhtStateFile) {
        if (dhtStarted.get()) {
            validateLibrary();
            lock.lock();
            try {
                libTorrent.save_dht_state(dhtStateFile);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean isUPnPStarted() {
        return upnpStarted.get();
    }

    @Override
    public void startUPnP() {
        validateLibrary();
        lock.lock();
        try {
            libTorrent.start_upnp();
            libTorrent.start_natpmp();
            upnpStarted.set(true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stopUPnP() {
        validateLibrary();
        lock.lock();
        try {
            libTorrent.stop_upnp();
            libTorrent.stop_natpmp();
            upnpStarted.set(false);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setPeerProxy(ProxySetting proxy) {
        validateLibrary();
        LibTorrentProxySetting proxySetting = null;
        if (proxy != null) {
            proxySetting = new LibTorrentProxySetting(proxy);
        } else {
            proxySetting = LibTorrentProxySetting.nullProxy();
        }
        lock.lock();
        try {
            libTorrent.set_peer_proxy(proxySetting);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setDHTProxy(ProxySetting proxy) {
        validateLibrary();
        LibTorrentProxySetting proxySetting = null;
        if (proxy != null) {
            proxySetting = new LibTorrentProxySetting(proxy);
        } else {
            proxySetting = LibTorrentProxySetting.nullProxy();
        }
        lock.lock();
        try {
            libTorrent.set_dht_proxy(proxySetting);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setTrackerProxy(ProxySetting proxy) {
        validateLibrary();
        LibTorrentProxySetting proxySetting = null;
        if (proxy != null) {
            proxySetting = new LibTorrentProxySetting(proxy);
        } else {
            proxySetting = LibTorrentProxySetting.nullProxy();
        }
        lock.lock();
        try {
            libTorrent.set_tracker_proxy(proxySetting);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setWebSeedProxy(ProxySetting proxy) {
        validateLibrary();
        LibTorrentProxySetting proxySetting = null;
        if (proxy != null) {
            proxySetting = new LibTorrentProxySetting(proxy);
        } else {
            proxySetting = LibTorrentProxySetting.nullProxy();
        }
        lock.lock();
        try {
            libTorrent.set_web_seed_proxy(proxySetting);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void queueTrackerScrapeRequest(String hexSha1Urn, URI trackerUri, ScrapeCallback callback) {
        validateLibrary();
        lock.lock();
        try {
            TrackerScrapeRequestCallback trc = new TrackerScrapeRequestCallback(callback);
            libTorrent.queue_tracker_scrape_request(hexSha1Urn, trackerUri.toASCIIString(), trc);
        } finally {
            lock.unlock();
        }
    }
}
