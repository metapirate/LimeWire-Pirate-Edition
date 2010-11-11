package org.limewire.core.settings;

import java.io.File;
import java.util.Properties;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.ProbabilisticBooleanSetting;
import org.limewire.setting.PropertiesSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for downloads.
 */
public class DownloadSettings extends LimeProps {
    private DownloadSettings() {
    }

    /**
     * Boolean setting indicating whether the max download speed should be limited using the MAX_DOWNLOAD_SPEED setting.
     */
    public static final BooleanSetting LIMIT_MAX_DOWNLOAD_SPEED = FACTORY.createBooleanSetting("LIMIT_MAX_DOWNLOAD_SPEED", false);
    
    /**
     * Setting for the number of bytes/second to allow for all downloads.
     * 
     * Minimum of 10 KB/sec
     */
    public static final IntSetting MAX_DOWNLOAD_SPEED = FACTORY.createIntSetting("MAX_DOWNLOAD_SPEED", SpeedConstants.CABLE_SPEED_INT/8 * 1024, 10 * 1024, Integer.MAX_VALUE);

    /**
     * The maximum number of downstream kilobytes per second ever passed by this
     * node.
     */
    public static final IntSetting MAX_MEASURED_DOWNLOAD_KBPS = FACTORY.createExpirableIntSetting(
            "MAX_DOWNLOAD_BYTES_PER_SEC", 0);

    /**
     * The maximum number of simultaneous downloads to allow.
     */
    public static final IntSetting MAX_SIM_DOWNLOAD = FACTORY.createIntSetting("MAX_SIM_DOWNLOAD",
            8);

    /**
     * Enable/disable skipping of acks.
     */
    public static final BooleanSetting SKIP_ACKS = FACTORY.createRemoteBooleanSetting("SKIP_ACKS",
            true);

    /**
     * Various parameters of the formulas for skipping acks.
     */
    public static final IntSetting MAX_SKIP_ACKS = FACTORY.createIntSetting(
            "MAX_SKIP_ACKS_2", 2);

    public static final FloatSetting DEVIATION = FACTORY.createRemoteFloatSetting("SKIP_DEVIATION",
            1.3f);

    public static final IntSetting PERIOD_LENGTH = FACTORY.createRemoteIntSetting("PERIOD_LENGTH",
            500);

    public static final IntSetting HISTORY_SIZE = FACTORY.createRemoteIntSetting("HISTORY_SIZE",
            10);

    /**
     * Whether the client should use HeadPings when ranking sources.
     */
    public static final BooleanSetting USE_HEADPINGS = FACTORY.createRemoteBooleanSetting(
            "USE_HEADPINGS", true);

    /**
     * Whether the client should drop incoming HeadPings.
     */
    public static final BooleanSetting DROP_HEADPINGS = FACTORY.createRemoteBooleanSetting(
            "DROP_HEADPINGS", false);

    /**
     * We should stop issuing HeadPings when we have this many verified sources.
     */
    public static final IntSetting MAX_VERIFIED_HOSTS = FACTORY.createRemoteIntSetting(
            "MAX_VERIFIED_HOSTS", 1);

    /**
     * We should not schedule more than this many head pings at once.
     */
    public static final IntSetting PING_BATCH = FACTORY.createRemoteIntSetting("PING_BATCH", 10);

    /**
     * Do not start new workers more than this often.
     */
    public static final IntSetting WORKER_INTERVAL = FACTORY.createIntSetting("WORKER_INTERVAL",
            2000);

    /** The maximum number of headers we'll read when parsing a download. */
    public static final IntSetting MAX_HEADERS = FACTORY.createRemoteIntSetting(
            "MAX_DOWNLOAD_HEADERS", 20);

    /** The maximum size of a single header we'll read when parsing a download. */
    public static final IntSetting MAX_HEADER_SIZE = FACTORY.createRemoteIntSetting(
            "MAX_DOWWNLOAD_HEADER_SIZE", 2048);

    /**
     * Use a download SelectionStrategy tailored for previewing if the file's
     * extension is in this list.
     */
    private static String[] defaultPreviewableExtensions = { "html", "htm", "xml", "txt", "rtf",
            "tex", "mp3", "mp4", "wav", "au", "aif", "aiff", "ra", "ram", "wma", "wmv", "midi",
            "aifc", "snd", "mpg", "mpeg", "asf", "qt", "mov", "avi", "mpe", "ogg", "rm", "m4a",
            "flac", "fla", "flv" };

    public static final StringArraySetting PREVIEWABLE_EXTENSIONS = FACTORY
            .createRemoteStringArraySetting("PREVIEWABLE_EXTENSIONS", defaultPreviewableExtensions);

    /** Whether to report disk problems to the bug server. */
    public static final ProbabilisticBooleanSetting REPORT_DISK_PROBLEMS = FACTORY
            .createRemoteProbabilisticBooleanSetting("REPORT_HTTP_DISK_PROBLEMS", 0f);

    /**
     * Whether or not to remember recently completed downloads.
     */
    public static final BooleanSetting REMEMBER_RECENT_DOWNLOADS = FACTORY.createBooleanSetting(
            "REMEMBER_RECENT_DOWNLOADS", true);

    /**
     * List of recent downloads.
     */
    public static final FileSetSetting RECENT_DOWNLOADS = FACTORY.createFileSetSetting(
            "RECENT_DOWNLOADS", new File[0]);

    /**
     * Whether or not to delete incomplete files when a download is canceled.
     */
    public static final BooleanSetting DELETE_CANCELED_DOWNLOADS = FACTORY.createBooleanSetting(
            "DELETE_CANCELED_DOWNLOADS", true);

    public static final StringSetting VIRUS_UPDATES_SERVER =
        FACTORY.createStringSetting("DownloadSettings.virusUpdatesServer",
        "http://af.avg.com/softw/90free/sdklmw/");

    public static final StringSetting VIRUS_NFO_SERVER =
        FACTORY.createStringSetting("DownloadSettings.virusNfoServer",
        "http://update.avg.com/softw/90free/sdklmw/");

    public static final IntSetting NUM_SCANNED_CLEAN = FACTORY.createIntSetting("NUM_SCANNED_CLEAN", 0);
    
    public static final IntSetting NUM_SCANNED_INFECTED = FACTORY.createIntSetting("NUM_SCANNED_INFECTED", 0);

    public static final PropertiesSetting INFECTED_EXTENSIONS = FACTORY.createPropertiesSetting("INFECTED_EXTENSIONS", new Properties());

    public static final IntSetting NUM_SCANS_FAILED = FACTORY.createIntSetting("NUM_SCANS_FAILED", 0);
    
    public static final IntSetting NUM_AV_INCREMENTAL_UPDATES_FAILED = FACTORY.createIntSetting("NUM_AV_INCREMENTAL_UPDATES_FAILED", 0);
    
    public static final IntSetting NUM_AV_INCREMENTAL_UPDATES_SUCCEEDED = FACTORY.createIntSetting("NUM_AV_INCREMENTAL_UPDATES_SUCCEEDED", 0);
    
    public static final IntSetting NUM_AV_FULL_UPDATES_FAILED = FACTORY.createIntSetting("NUM_AV_FULL_UPDATES_FAILED", 0);
    
    public static final IntSetting NUM_AV_FULL_UPDATES_SUCCEEDED = FACTORY.createIntSetting("NUM_AV_FULL_UPDATES_SUCCEEDED", 0);
    
    public static final IntSetting NUM_AV_MEMENTOS_RESUMED = FACTORY.createIntSetting("NUM_AV_MEMENTOS_RESUMED", 0);
}
