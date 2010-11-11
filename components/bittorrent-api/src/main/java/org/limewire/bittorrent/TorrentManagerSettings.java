package org.limewire.bittorrent;

import java.io.File;
import java.util.List;

public interface TorrentManagerSettings {

    /**
     * Returns the upload rate limit for libtorrent in bytes/second. A value of
     * 0 is means it is unlimited.
     */
    public int getMaxUploadBandwidth();

    /**
     * Returns the download rate limit for libtorrent in bytes/second. A value
     * of 0 is means it is unlimited.
     */
    public int getMaxDownloadBandwidth();

    /**
     * Returns true if the Torrent capabilities are enabled.
     */
    public boolean isTorrentsEnabled();

    /**
     * Returns true if the setting to report library load failures is turned on.
     */
    boolean isReportingLibraryLoadFailture();

    /**
     * Returns the listening start port.
     */
    public int getListenStartPort();

    /**
     * Returns the listening end port.
     */
    public int getListenEndPort();

    /**
     * Default seed ratio to have considered met seeding criteria.
     */
    public float getSeedRatioLimit();

    /**
     * Default seed time over download time to have considered met seeding
     * criteria.
     */
    public float getSeedTimeRatioLimit();

    /**
     * Default amount of seed time to have considered having met seeding
     * criteria.
     */
    public int getSeedTimeLimit();

    /**
     * Returns the limit for the active number of active managed torrent
     * downloads.
     */
    public int getActiveDownloadsLimit();

    /**
     * Returns the limit for the active number of active managed torrent seeds.
     */
    public int getActiveSeedsLimit();

    /**
     * Returns the limit for the total number of active managed torrents.
     */
    public int getActiveLimit();

    /**
     * Returns the alert mask for the torrent session.
     */
    public int getAlertMask();

    /**
     * Returns the file the dht state used for bootstrapping he dht will be
     * stored it.
     */
    public File getDHTStateFile();

    /**
     * Returns a list of bootstrapping routers for the dht.
     */
    public List<TorrentIpPort> getBootStrapDHTRouters();

    /**
     * Returns the network interface Torrents should. May return null to have
     * one chosen by the operating system.
     */
    public String getListenInterface();
}