package org.limewire.core.settings;

import java.io.File;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.CommonUtils;

/**
 * BitTorrent settings.
 */
public class BittorrentSettings extends LimeProps {

    private BittorrentSettings() {
        // empty constructor
    }

    /**
     * Whether to show a popup dialog allowing the user to select files within
     * the torrent to download prior to starting the download.
     */
    public static final BooleanSetting TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING = FACTORY
            .createBooleanSetting("TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING", true);

    /**
     * Setting for whether or not be want to report issues loading the
     * libtorrent libraries.
     */
    public static final BooleanSetting LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE = FACTORY
            .createRemoteBooleanSetting("LIBTORRENT_REPORT_LIBRARY_LOAD_FAILURE", false);

    /**
     * Setting for whether or not torrents should use UPNP.
     */
    public static final BooleanSetting TORRENT_USE_UPNP = FACTORY.createBooleanSetting(
            "TORRENT_USE_UPNP", true);

    /**
     * Whether or not libtorrent is enabled and we should try loading the
     * libtorrent libraries.
     */
    public static final BooleanSetting LIBTORRENT_ENABLED = FACTORY.createBooleanSetting(
            "LIBTORRENT_ENABLED", true);

    /**
     * The start listening port for listening for bittorrent connections.
     * Libtorrent picks the first available port with in the range of the start
     * to end ports.
     */
    public static final IntSetting LIBTORRENT_LISTEN_START_PORT = FACTORY.createIntSetting(
            "LIBTORRENT_LISTEN_START_PORT", 6881);

    /**
     * The end listening port for listening for bittorrent connections.
     * Libtorrent picks the first available port with in the range of the start
     * to end ports.
     */
    public static final IntSetting LIBTORRENT_LISTEN_END_PORT = FACTORY.createIntSetting(
            "LIBTORRENT_LISTEN_END_PORT", 6889);

    /**
     * The folder where all the upload mementos are saved for torrents.
     */
    public static FileSetting TORRENT_UPLOADS_FOLDER = FACTORY.createFileSetting(
            "TORRENT_UPLOADS_FOLDER", new File(CommonUtils.getUserSettingsDir(), "uploads.dat/"));

    /**
     * The target seed ratio for torrents. Torrents which have met this ratio
     * will be removed. This number is also used via the libtorrent queuing
     * algorithm when trying to decide to queue/dequeue automanaged torrents.
     * This limit only effect automanaged torrents.
     * 
     * When the UPLOAD_TORRENTS_FOREVER setting is set to true, no matter what
     * the value of this setting is, it will pass the maximum value to
     * libtorrent.
     */
    public static final FloatSetting LIBTORRENT_SEED_RATIO_LIMIT = FACTORY.createFloatSetting(
            "LIBTORRENT_SEED_RATIO_LIMIT", 2.0f, 0.00f, Float.MAX_VALUE);

    /**
     * The target seed time ratio limit. The amount of time trying to seed over
     * the amount of time it took to download the torrent. This number is also
     * used via the libtorrent queuing algorithm when trying to decide to
     * queue/dequeue automanaged torrents. This limit only effect automanaged
     * torrents.
     */
    public static final FloatSetting LIBTORRENT_SEED_TIME_RATIO_LIMIT = FACTORY.createFloatSetting(
            "LIBTORRENT_SEED_TIME_RATIO_LIMIT", 7f);

    /**
     * The target seed time limit. The amount time the torrent will actively try
     * to be seeded for. This number is also used via the libtorrent queuing
     * algorithm when trying to decide to queue/dequeue automanaged torrents.
     * This limit only effect automanaged torrents, but will take other torrents
     * into account as we.
     * 
     * When the UPLOAD_TORRENTS_FOREVER setting is set to true, no matter what
     * the value of this setting is, it will pass the maximum value to
     * libtorrent.
     */
    public static final IntSetting LIBTORRENT_SEED_TIME_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_SEED_TIME_LIMIT", 60 * 60 * 24, 0, Integer.MAX_VALUE);// 24

    // hours
    // default

    /**
     * The total number of active downloads limit. This number is also used via
     * the libtorrent queuing algorithm when trying to decide to queue/dequeue
     * automanaged torrents. This limit only effect automanaged torrents, but
     * will take other torrents into account as we.
     */
    public static final IntSetting LIBTORRENT_ACTIVE_DOWNLOADS_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_ACTIVE_DOWNLOADS_LIMIT", 8);

    /**
     * The total number of active seeds limit. This number is also used via the
     * libtorrent queuing algorithm when trying to decide to queue/dequeue
     * automanaged torrents. This limit only effect automanaged torrents, but
     * will take other torrents into account as we.
     */
    public static final IntSetting LIBTORRENT_ACTIVE_SEEDS_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_ACTIVE_SEEDS_LIMIT", 5);

    /**
     * The total number of active torrents limit. This number is also used via
     * the libtorrent queuing algorithm when trying to decide to queue/dequeue
     * automanaged torrents. This limit only effect automanaged torrents, but
     * will take other torrents into account as we.
     */
    public static final IntSetting LIBTORRENT_ACTIVE_LIMIT = FACTORY.createIntSetting(
            "LIBTORRENT_ACTIVE_LIMIT", 15);

    /**
     * The maximum number of torrents that can be seeded at once. One the limit
     * is reached seeding torrents will be stopped.
     */
    public static final IntSetting TORRENT_SEEDING_LIMIT = FACTORY.createRemoteIntSetting(
            "TORRENT_SEEDING_LIMIT", 40);

    /**
     * This setting will cause torrents to upload forever, and will not limit
     * how long or to what seed ratio the torrents will seed.
     */
    public static final BooleanSetting UPLOAD_TORRENTS_FOREVER = FACTORY.createBooleanSetting(
            "UPLOAD_TORRENTS_FOREVER", false);

    /**
     * The file that previous dht states have been stored in.
     */
    public static final FileSetting LIBTORRENT_DHT_STATE = FACTORY.createFileSetting(
            "LIBTORRENT_DHT_STATE", new File(CommonUtils.getUserSettingsDir(),
                    "libtorrent/libtorrentdht.dat"));

    /**
     * Contains a list of dht router address and ip pairs to allow libtorrent to
     * find dht nodes, when we have none.
     */
    public static final StringArraySetting TORRENT_BOOTSTRAP_DHT_ROUTERS = FACTORY
            .createRemoteStringArraySetting("TORRENT_DHT_ROUTERS", new String[] {
                    "router.bittorrent.com:6881", "router.utorrent.com:6881",
                    "router.bitcomet.com:6881" });

}
