package org.limewire.core.settings;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.limewire.core.api.Category;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.Setting;
import org.limewire.setting.StringSetting;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.SystemUtils.SpecialLocations;

/**
 * Settings for sharing.
 */
public class SharingSettings extends LimeProps {

    private SharingSettings() {
    }

    /**
     * Stores the download directory file settings for each media type by its
     * description key {@link Category#getSchemaName()}. The settings are
     * loaded lazily during the first request.
     */
    private static final Hashtable<String, FileSetting> downloadDirsByDescription = new Hashtable<String, FileSetting>();

    public static final File DEFAULT_SAVE_DIR = new File(getLimeWireRootFolder(), "Saved");

    public static final File DEFAULT_SHARE_DIR = new File(getLimeWireRootFolder(), "Shared");

    /**
     * Default directory for songs purchased from LWS.
     */
    public static final File DEFAULT_SAVE_LWS_DIR = new File(getLimeWireRootFolder(),
            "Store Purchased");

    public static final String DEFAULT_LWS_FILENAME_TEMPLATE = "<artist> - <album> - <track> - <title>";

    public static final String DEFAULT_LWS_FOLDER_TEMPLATE = "<artist>" + File.separatorChar + "<album>";

    /**
     * Whether or not we're going to add an alternate for ourselves to our
     * shared files. Primarily set to false for testing.
     */
    public static final BooleanSetting ADD_ALTERNATE_FOR_SELF = FACTORY.createBooleanSetting(
            "ADD_ALTERNATE_FOR_SELF", true);

    /**
     * The directory for saving files.
     */
    public static final FileSetting DIRECTORY_FOR_SAVING_FILES = FACTORY.createFileSetting(
            "DIRECTORY_FOR_SAVING_FILES", DEFAULT_SAVE_DIR).setAlwaysSave(true);
    
    /**
     * Template for substructure when saving songs purchased from LimeWire Store
     * (LWS). The template allows purchased songs to be saved in a unique
     * fashion, ie. LWS_dir/artist/album/songX.mp3.
     */
    public static final StringSetting TEMPLATE_SUBDIRECTORY_LWS_FILES = FACTORY
            .createStringSetting("TEMPLATE_FOR_SAVING_LWS_FILES", DEFAULT_LWS_FOLDER_TEMPLATE);

    /**
     * Template for file name structure when saving songs purchased from the
     * LimeWire Store (LWS). The template allows purchased songs to be named in a
     * unique fashion based on the songs meta data ie. artist - track # -
     * title.mp3.
     */
    public static final StringSetting TEMPLATE_FOR_NAMING_LWS_FILES = FACTORY.createStringSetting(
            "TEMPLATE_FOR_NAMING_LWS_FILES", DEFAULT_LWS_FILENAME_TEMPLATE);

    /**
     * The directory where incomplete files are stored (downloads in progress).
     */
    public static final FileSetting INCOMPLETE_DIRECTORY = FACTORY.createFileSetting(
            "INCOMPLETE_DIRECTORY", (new File(DIRECTORY_FOR_SAVING_FILES.get().getParent(),
                    "Incomplete")));

    /**
     * A file with a snapshot of current downloading files.
     */
    public static final FileSetting OLD_DOWNLOAD_SNAPSHOT_FILE = FACTORY.createFileSetting(
            "DOWNLOAD_SNAPSHOT_FILE", (new File(INCOMPLETE_DIRECTORY.get(), "downloads.dat")));

    /**
     * A file with a snapshot of current downloading files.
     */
    public static final FileSetting OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE = FACTORY.createFileSetting(
            "DOWNLOAD_SNAPSHOT_BACKUP_FILE",
            (new File(INCOMPLETE_DIRECTORY.get(), "downloads.bak")));

    /**
     * The minimum age in days for which incomplete files will be deleted. This
     * values may be zero or negative; doing so will cause LimeWire to delete
     * ALL incomplete files on startup.
     */
    public static final IntSetting INCOMPLETE_PURGE_TIME = FACTORY.createIntSetting(
            "INCOMPLETE_PURGE_TIME", 7);

    /**
     * The time, in days, after which .torrent meta data files are deleted.
     */
    public static final IntSetting TORRENT_METADATA_PURGE_TIME = FACTORY.createIntSetting(
            "TORRENT_METADATA_PURGE_TIME", 7);

    /**
     * Specifies whether or not completed downloads should automatically be
     * cleared from the download window.
     */
    public static final BooleanSetting CLEAR_DOWNLOAD = FACTORY.createBooleanSetting(
            "CLEAR_DOWNLOAD", false);
    
    /**
     * Specifies whether or not to warn the user when they are about to share a folder.
     */
    public static final BooleanSetting WARN_SHARING_FOLDER = FACTORY.createBooleanSetting(
            "WARN_SHARING_FOLDER", true);
    
    /**
     * Specifies whether or not to warn the user when they are sharing documents with the world.
     */
    public static final BooleanSetting WARN_SHARING_DOCUMENTS_WITH_WORLD = FACTORY.createBooleanSetting(
            "WARN_SHARING_DOCUMENTS_WITH_WORLD", true);
    
    /**
     * Helper method left from SettingsManager.
     * <p>
     * Sets the directory for saving files.
     * <p>
     * <b>Modifies:</b> DIRECTORY_FOR_SAVING_FILES, INCOMPLETE_DIRECTORY,
     * DOWNLOAD_SNAPSHOT_FILE
     * 
     * @param saveDir a <tt>File</tt> instance denoting the abstract pathname of
     *        the directory for saving files.
     * 
     * @throws <tt>IOException</tt> if the directory denoted by the directory
     *         pathname String parameter did not exist prior to this method call
     *         and could not be created, or if the canonical path could not be
     *         retrieved from the file system.
     * 
     * @throws <tt>NullPointerException</tt> If the "dir" parameter is null.
     */
    public static final void setSaveDirectory(File saveDir) throws IOException {
        if (saveDir == null)
            throw new NullPointerException();
        if (!saveDir.isDirectory()) {
            if (!saveDir.mkdirs())
                throw new IOException("could not create save dir");
        }

        String parentDir = saveDir.getParent();
        File incDir = new File(parentDir, "Incomplete");
        if (!incDir.isDirectory()) {
            if (!incDir.mkdirs())
                throw new IOException("could not create incomplete dir");
        }

        FileUtils.setWriteable(saveDir);
        FileUtils.setWriteable(incDir);

        if (!saveDir.canRead() || !FileUtils.canWrite(saveDir) || !incDir.canRead()
                || !FileUtils.canWrite(incDir)) {
            throw new IOException("could not write to selected directory");
        }

        // Canonicalize the files ...
        try {
            saveDir = FileUtils.getCanonicalFile(saveDir);
        } catch (IOException ignored) {
        }
        try {
            incDir = FileUtils.getCanonicalFile(incDir);
        } catch (IOException ignored) {
        }
        File snapFile = new File(incDir, "downloads.dat");
        try {
            snapFile = FileUtils.getCanonicalFile(snapFile);
        } catch (IOException ignored) {
        }
        File snapBackup = new File(incDir, "downloads.bak");
        try {
            snapBackup = FileUtils.getCanonicalFile(snapBackup);
        } catch (IOException ignored) {
        }

        DIRECTORY_FOR_SAVING_FILES.set(saveDir);
        INCOMPLETE_DIRECTORY.set(incDir);
        OLD_DOWNLOAD_SNAPSHOT_FILE.set(snapFile);
        OLD_DOWNLOAD_SNAPSHOT_BACKUP_FILE.set(snapBackup);
    }

    /**
     * Retrieves the default save directory for a given file name. Getting the
     * save directory for null will result in the default save directory.
     */
    public static final File getSaveDirectory(Category category) {
        if(category == null) {
            return DIRECTORY_FOR_SAVING_FILES.get();
        } else {
            FileSetting fs = getFileSettingForCategory(category);
            if (fs.isDefault()) {
                return DIRECTORY_FOR_SAVING_FILES.get();
            }
            return fs.get();
        }
    }
    
    public static final File getSaveDirectory() {
        return DIRECTORY_FOR_SAVING_FILES.get();
    }

    /**
     * Sets the template for creating sub directories of store files using
     * metadata.
     * 
     * @param template the template that describes the sub directory structure
     * @throws NullPointerException if template is null
     */
    public static final void setSubdirectoryLWSTemplate(String template) {
        if (template == null)
            throw new NullPointerException();
        TEMPLATE_SUBDIRECTORY_LWS_FILES.set(template);
    }

    /**
     * @return template of how to create subdirectories of store files If no
     *         subdirectory template is used, will return ""
     */
    public static final String getSubDirectoryLWSTemplate() {
        return TEMPLATE_SUBDIRECTORY_LWS_FILES.get();
    }

    /**
     * Sets the template for naming files purchased from the LWS.
     * 
     * @param template the template that describes how id3 information should be
     *        used to name a Store file
     * @throws NullPointerException if template is null
     */
    public static final void setFileNameLWSTemplate(String template) {
        if (template == null)
            throw new NullPointerException();
        TEMPLATE_FOR_NAMING_LWS_FILES.set(template);
    }

    /**
     * @return template of how to name LWS files.
     */
    public static final String getFileNameLWSTemplate() {
        return TEMPLATE_FOR_NAMING_LWS_FILES.get();
    }

    /*********************************************************************/

    /**
     * Whether or not to auto-share files when using 'Download As'.
     */
    public static final BooleanSetting SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES = FACTORY
            .createBooleanSetting("SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES", true);

    /**
     * Sets the probability (expressed as a percentage) that an incoming
     * freeloader will be accepted. For example, if allowed==50, an incoming
     * connection has a 50-50 chance being accepted. If allowed==100, all
     * incoming connections are accepted.
     */
    public static final IntSetting FREELOADER_ALLOWED = FACTORY.createIntSetting(
            "FREELOADER_ALLOWED_2", 100);

    /**
     * Minimum the number of files a host must share to not be considered a
     * freeloader. For example, if files==0, no host is considered a freeloader.
     */
    public static final IntSetting FREELOADER_FILES = FACTORY.createIntSetting(
            "FREELOADER_FILES_2", 1);

    /**
     * The timeout value for persistent HTTP connections in milliseconds.
     */
    public static final IntSetting PERSISTENT_HTTP_CONNECTION_TIMEOUT = FACTORY.createIntSetting(
            "PERSISTENT_HTTP_CONNECTION_TIMEOUT", 15000);

    /**
     * Specifies whether or not completed uploads should automatically be
     * cleared from the upload window.
     */
    public static final BooleanSetting CLEAR_UPLOAD = FACTORY.createBooleanSetting("CLEAR_UPLOAD",
            true);

    /**
     * Whether or not browsers should be allowed to perform uploads.
     */
    public static final BooleanSetting ALLOW_BROWSER = FACTORY.createBooleanSetting(
            "ALLOW_BROWSER", false);

    /**
     * Whether to throttle hashing of shared files.
     */
    public static final BooleanSetting FRIENDLY_HASHING = FACTORY.createBooleanSetting(
            "FRIENDLY_HASHING", true);

    /**
     * Minimum idle time before we start to hash at full throttle.
     */
    public static final IntSetting MIN_IDLE_TIME_FOR_FULL_HASHING = FACTORY.createIntSetting(
            "MIN_IDLE_TIME_FOR_FULL_HASHING", 5 * 60 * 1000);

    /**
     * Setting for the threshold of when to warn the user that a lot of files
     * are being shared.
     */
    public static final IntSetting FILES_FOR_WARNING = FACTORY.createIntSetting(
            "FILES_FOR_WARNING", 1000);

    /**
     * Setting for the threshold of when to warn the user that a lot of files
     * are being shared.
     */
    public static final IntSetting DEPTH_FOR_WARNING = FACTORY.createIntSetting(
            "DEPTH_FOR_WARNING", 4);

    /**
     * Returns the download directory file setting for a mediatype. The settings
     * are created lazily when they are requested for the first time. The
     * default download directory is a file called "invalidfile" the file
     * setting should not be used when its {@link Setting#isDefault()} returns
     * true. Use {@link #DIRECTORY_FOR_SAVING_FILES} instead then.
     * 
     * @param type the mediatype for which to look up the file setting
     * @return the filesetting for the media type
     */
    public static final FileSetting getFileSettingForCategory(Category category) {
        String schema = category.getSchemaName();
        FileSetting setting = downloadDirsByDescription.get(schema);
        if (setting == null) {
            setting = FACTORY.createProxyFileSetting("DIRECTORY_FOR_SAVING_" + schema
                    + "_FILES", DIRECTORY_FOR_SAVING_FILES);
            downloadDirsByDescription.put(schema, setting);
        }
        return setting;
    }

    /**
     * The Creative Commons explanation URL.
     */
    public static final StringSetting CREATIVE_COMMONS_INTRO_URL = FACTORY
            .createRemoteStringSetting("CREATIVE_COMMONS_URL",
                    "http://creativecommons.org/about/licenses/how1");

    /**
     * The Creative Commons verification explanation URL.
     */
    public static final StringSetting CREATIVE_COMMONS_VERIFICATION_URL = FACTORY
            .createRemoteStringSetting("CREATIVE_COMMONS_VERIFICATION_URL",
                    "http://creativecommons.org/technology/embedding#2");

    /**
     * Setting for whether or not to allow partial files to be shared.
     */
    public static final BooleanSetting ALLOW_PARTIAL_SHARING = FACTORY.createBooleanSetting(
            "ALLOW_PARTIAL_SHARING", true);

    /**
     * Remote switch to turn off partial results.
     */
    public static final BooleanSetting ALLOW_PARTIAL_RESPONSES = FACTORY
            .createRemoteBooleanSetting("ALLOW_PARTIAL_RESPONSES", true);

    /**
     * Maximum size in bytes for the encoding of available ranges per Response
     * object.
     */
    public static final IntSetting MAX_PARTIAL_ENCODING_SIZE = FACTORY.createRemoteIntSetting(
            "MAX_PARTIAL_ENCODING_SIZE", 20);

    /**
     * Whether to publish keywords from partial files in the qrp.
     */
    public static final BooleanSetting PUBLISH_PARTIAL_QRP = FACTORY.createRemoteBooleanSetting(
            "PUBLISH_PARTIAL_QRP", true);

    /**
     * Whether to load keywords from incomplete files in the trie.
     */
    public static final BooleanSetting LOAD_PARTIAL_KEYWORDS = FACTORY.createRemoteBooleanSetting(
            "LOAD_PARTIAL_KEYWORDS", true);

    public static final StringSetting LAST_WARNED_SAVE_DIRECTORY = FACTORY.createStringSetting(
            "LAST_WARNED_SAVED_DIRECTORY", "");

    /**
     * Returns the root folder from which all Saved/Shared/etc.. folders should
     * be placed.
     */
    private static File getLimeWireRootFolder() {
        String root = null;

        if (OSUtils.isWindows()) {
            root = SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS);
        }

        if (root == null || "".equals(root))
            root = CommonUtils.getUserHomeDir().getPath();

        return new File(root, "LimeWire");
    }    

}
