package org.limewire.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;

/**
 * The torrent manager allows for adding an removing torrents, as well as
 * controlling shared torrent features.
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
public interface TorrentManager {

    /**
     * Removes torrent from control of the torrent manager. Also delegates a
     * remove torrent call to the libtorrent wrapper library.
     */
    public void removeTorrent(Torrent torrent);

    /**
     * Registers the specified torrent with the TorrentManager. Delegates an add
     * torrent call to the underlying libtorrent wrapper library.
     * 
     * Returns the torrent if it was successfully added, null otherwise.
     */
    public Torrent addTorrent(TorrentParams params) throws IOException;

    /**
     * Returns the torrent for the given torrent file if found. null otherwise.
     */
    public Torrent getTorrent(File torrentFile);

    /**
     * Returns the torrent for the given sha1 if found. null otherwise.
     */
    public Torrent getTorrent(String sha1);

    /**
     * Returns true if the TorrentManager loaded properly and is in a valid
     * state.
     */
    public boolean isValid();

    /**
     * Returns true if the torrent manager currently had a torrent registered
     * with the specified torrent file, and the torrent is not finished
     * downloading yet.
     */
    public boolean isDownloadingTorrent(File torrentFile);

    /**
     * Updates the torrent manager with any new setting values.
     */
    void setTorrentManagerSettings(TorrentManagerSettings settings);

    /**
     * Returns the current TorrentSettings object set on the torrent session.
     */
    public TorrentManagerSettings getTorrentManagerSettings();

    /**
     * Starts the torrent manager, and its necessary worker threads.
     */
    public void start();

    /**
     * Shuts down the torrent manager, and any needed threads.
     */
    public void stop();

    /**
     * Initializes this torrent manager.
     */
    public void initialize();

    /**
     * Set a way to filter peers based on IP Address, where IP is represented as
     * network byte order integer
     * 
     * @param ipFilter the filter to apply
     */
    public void setIpFilter(TorrentIpFilter ipFilter);

    /**
     * Returns true if the torrent manager is initialized.
     */
    public boolean isInitialized();

    /**
     * Returns a snapshot list of the torrents that are currently in the torrent
     * manager.
     */
    public List<Torrent> getTorrents();

    /**
     * Returns a lock for the torrent manager. Locking is only required if state
     * needs to be consistent across multiple method calls. Otherwise locking is
     * done internally for single method calls.
     */
    public Lock getLock();

    /**
     * Stops the mainline bittorrent dht.
     */
    public void stopDHT();

    /**
     * Starts the mainline bittorrent dht. Using the given file to help
     * bootstrap its state.
     */
    public void startDHT(File dhtStateFile);

    /**
     * Starts upnp for the torrents.
     */
    public void startUPnP();

    /**
     * Stops upnp for the torrents.
     */
    public void stopUPnP();

    /**
     * Returns true if upnp has been started.
     */
    public boolean isUPnPStarted();

    /**
     * Returns true if the dht has been started.
     */
    public boolean isDHTStarted();

    /**
     * Saves the current dht state into the given file.
     */
    public void saveDHTState(File dhtStateFile);

    /**
     * Sets the peer proxy for this torrent manager. Trying to set to an http
     * proxy will have the same effect as passing in a null proxy. The proxy
     * will be removed.
     */
    public void setPeerProxy(ProxySetting proxy);

    /**
     * Sets the web seed proxy for this torrent manager. The proxy can be
     * removed by setting to null.
     */
    public void setWebSeedProxy(ProxySetting proxy);

    /**
     * Sets the dht proxy for this torrent manager. Trying to set to an http
     * proxy will have the same effect as passing in a null proxy. The proxy
     * will be removed.
     */
    public void setDHTProxy(ProxySetting proxy);

    /**
     * Sets the tracker proxy for this torrent manager. It only effects http
     * tracker connections if the proxy is an http proxy. If the proxy supports
     * udp, udp tracker connections will be effected as well. The proxy can be
     * removed by setting to null.
     */
    public void setTrackerProxy(ProxySetting proxy);

    public void queueTrackerScrapeRequest(String hexSha1Urn, URI trackerUri, ScrapeCallback callback);
}
