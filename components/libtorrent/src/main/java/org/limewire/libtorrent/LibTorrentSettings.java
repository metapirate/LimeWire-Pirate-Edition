package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentManagerSettings;

import com.sun.jna.Structure;

public class LibTorrentSettings extends Structure {

    /**
     * upload rate limit for libtorrent in bytes/second. A value of 0 is means
     * it is unlimited.
     */
    public int max_upload_bandwidth = 0;

    /**
     * Download rate limit for libtorrent in bytes/second. A value of 0 is means
     * it is unlimited.
     */
    public int max_download_bandwidth = 0;

    public int listen_start_port = 6881;

    public int listen_end_port = 6889;

    public float seed_ratio_limit;

    public float seed_time_ratio_limit;

    public int seed_time_limit;

    public int active_downloads_limit;

    public int active_seeds_limit;

    public int active_limit;

    public int alert_mask;
    
    public String listen_interface;

    public LibTorrentSettings(TorrentManagerSettings torrentSettings) {
        this.max_upload_bandwidth = torrentSettings.getMaxUploadBandwidth();
        this.max_download_bandwidth = torrentSettings.getMaxDownloadBandwidth();
        this.listen_start_port = torrentSettings.getListenStartPort();
        this.listen_end_port = torrentSettings.getListenEndPort();
        this.seed_ratio_limit = torrentSettings.getSeedRatioLimit();
        this.seed_time_ratio_limit = torrentSettings.getSeedTimeRatioLimit();
        this.seed_time_limit = torrentSettings.getSeedTimeLimit();
        this.active_downloads_limit = torrentSettings.getActiveDownloadsLimit();
        this.active_seeds_limit = torrentSettings.getActiveSeedsLimit();
        this.active_limit = torrentSettings.getActiveLimit();
        this.alert_mask = torrentSettings.getAlertMask();
        this.listen_interface = torrentSettings.getListenInterface();
    }
}
