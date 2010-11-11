package com.limegroup.bittorrent;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.limewire.bittorrent.TorrentIpPort;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.io.InvalidDataException;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.io.UnresolvedIpPortImpl;
import org.limewire.libtorrent.LibTorrentAlert;
import org.limewire.libtorrent.LibTorrentIpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Implements the TorrentSetting interface with limewire specific settings.
 */
class LimeWireTorrentSettings implements TorrentManagerSettings {
    private static final Log LOG = LogFactory.getLog(LimeWireTorrentSettings.class);

    @Override
    public int getMaxDownloadBandwidth() {
        if (!DownloadSettings.LIMIT_MAX_DOWNLOAD_SPEED.getValue()) {
            return 0;
        }
        return DownloadSettings.MAX_DOWNLOAD_SPEED.getValue();
    }

    @Override
    public int getMaxUploadBandwidth() {
        if (!UploadSettings.LIMIT_MAX_UPLOAD_SPEED.getValue()) {
            return 0;
        }
        return UploadSettings.MAX_UPLOAD_SPEED.getValue();
    }

    @Override
    public boolean isTorrentsEnabled() {
        return BittorrentSettings.LIBTORRENT_ENABLED.getValue();
    }

    @Override
    public boolean isReportingLibraryLoadFailture() {
        return BittorrentSettings.LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE.getValue();
    }

    @Override
    public int getListenStartPort() {
        return BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.getValue();
    }

    @Override
    public int getListenEndPort() {
        return BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.getValue();
    }

    @Override
    public float getSeedRatioLimit() {
        if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
            return Float.MAX_VALUE;
        }
        return BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getValue();
    }

    @Override
    public int getSeedTimeLimit() {
        if (BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue()) {
            return Integer.MAX_VALUE;
        }
        return BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getValue();
    }

    @Override
    public float getSeedTimeRatioLimit() {
        return BittorrentSettings.LIBTORRENT_SEED_TIME_RATIO_LIMIT.getValue();
    }

    @Override
    public int getActiveDownloadsLimit() {
        return BittorrentSettings.LIBTORRENT_ACTIVE_DOWNLOADS_LIMIT.getValue();
    }

    @Override
    public int getActiveLimit() {
        return BittorrentSettings.LIBTORRENT_ACTIVE_LIMIT.getValue();
    }

    @Override
    public int getActiveSeedsLimit() {
        return BittorrentSettings.LIBTORRENT_ACTIVE_SEEDS_LIMIT.getValue();
    }

    @Override
    public int getAlertMask() {
        return LibTorrentAlert.storage_notification | LibTorrentAlert.progress_notification
                | LibTorrentAlert.status_notification;
    }

    @Override
    public File getDHTStateFile() {
        return BittorrentSettings.LIBTORRENT_DHT_STATE.get();
    }

    @Override
    public List<TorrentIpPort> getBootStrapDHTRouters() {
        String[] routerStrings = BittorrentSettings.TORRENT_BOOTSTRAP_DHT_ROUTERS.get();
        List<TorrentIpPort> dhtRouters = new ArrayList<TorrentIpPort>();
        if (routerStrings != null) {
            for (int i = 0; i < routerStrings.length; i++) {
                String router = routerStrings[i];
                try {
                    UnresolvedIpPort unresolvedIpPort = new UnresolvedIpPortImpl(router);
                    unresolvedIpPort.resolve();
                    dhtRouters.add(new LibTorrentIpPort(unresolvedIpPort.getAddress(),
                            unresolvedIpPort.getPort()));
                } catch (UnknownHostException e) {
                    LOG.debugf(e, "Address not valid: {0}", router);
                } catch (InvalidDataException e) {
                    LOG.debugf(e, "Router not valid: {0}", router);
                }
            }
        }
        return dhtRouters;
    }

    @Override
    public String getListenInterface() {
        if (ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue()) {
            return ConnectionSettings.CUSTOM_INETADRESS.get();
        }
        return null;
    }
}