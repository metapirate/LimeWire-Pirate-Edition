package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

public class LibrarySettings extends LimeProps {
    
    /** True if documents can be shared with gnutella. */
    public static final BooleanSetting ALLOW_DOCUMENT_GNUTELLA_SHARING =
        FACTORY.createBooleanSetting("DOCUMENT_SHARING_ENABLED", false);
    
    /** True if programs are allowed in the library at all. */
    public static final BooleanSetting ALLOW_PROGRAMS =
        FACTORY.createBooleanSetting("PROGRAMS_ALLOWED", false);
    
    /** The current version of the library. */
    public static final StringSetting VERSION =
        FACTORY.createStringSetting("LIBRARY_VERSION", LibraryVersion.FOUR_X.name());
    
    /** True if the user should be prompted about what categories to share during a folder drop. */
    public static final BooleanSetting ASK_ABOUT_FOLDER_DROP_CATEGORIES =
        FACTORY.createBooleanSetting("ASK_ABOUT_FOLDER_DROP_CATEGORIES", true);

    /** When adding a folder, will recursively add subfolders if true, otherwise will just add top level folder. */
    public static final BooleanSetting DEFAULT_RECURSIVELY_ADD_FOLDERS_OPTION = 
        FACTORY.createBooleanSetting("RECURSIVELY_ADD_FOLDERS", true);
    
    public static enum LibraryVersion {
        FOUR_X, FIVE_0_0;
    }
    
    /** More extensions that belong in the audio category. */
    public static final StringArraySetting ADDITIONAL_AUDIO_EXTS =
        FACTORY.createRemoteStringArraySetting("ADDITIONAL_AUDIO_EXTS", new String[0]);
    
    /** More extensions that belong in the video category. */
    public static final StringArraySetting ADDITIONAL_VIDEO_EXTS =
        FACTORY.createRemoteStringArraySetting("ADDITIONAL_VIDEO_EXTS", new String[0]);
    
    /** More extensions that belong in the image category. */
    public static final StringArraySetting ADDITIONAL_IMAGE_EXTS =
        FACTORY.createRemoteStringArraySetting("ADDITIONAL_IMAGE_EXTS", new String[0]);
    
    /** More extensions that belong in the document category. */
    public static final StringArraySetting ADDITIONAL_DOCUMENT_EXTS =
        FACTORY.createRemoteStringArraySetting("ADDITIONAL_DOCUMENT_EXTS", new String[0]);
    
    /** More extensions that belong in the program category on Windows. */
    public static final StringArraySetting ADDITIONAL_PROGRAM_WINDOWS_EXTS =
        FACTORY.createRemoteStringArraySetting("ADDITIONAL_PROGRAM_WINDOWS_EXTS", new String[0]);
    
    /** More extensions that belong in the program category on OSX & Linux. */
    public static final StringArraySetting ADDITIONAL_PROGRAM_OSX_LINUX_EXTS =
        FACTORY.createRemoteStringArraySetting("ADDITIONAL_PROGRAM_OSX_LINUX_EXTS", new String[0]);
    
    /** More extensions that should be considered torrents, empty now. */
    public static final StringArraySetting ADDITIONAL_TORRENT_EXTS =
        FACTORY.createRemoteStringArraySetting("ADDITIONAL_TORRENT_EXTS", new String[0]);
    
}
