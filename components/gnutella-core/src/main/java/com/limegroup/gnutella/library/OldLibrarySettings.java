package com.limegroup.gnutella.library;

import java.io.File;

import org.limewire.core.settings.LimeProps;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/** List of all old library settings. */
@Deprecated
final class OldLibrarySettings extends LimeProps {
    
    private static final String DEFAULT_EXTENSIONS_TO_SHARE =
        "asx;html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;mp4;wav;wax;au;aif;aiff;"+
        "ra;ram;wma;wm;wmv;mp2v;mlv;mpa;mpv2;mid;midi;rmi;aifc;snd;flac;fla;"+
        "mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;bmp;"+
        "exe;zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi;img;ogg;rm;"+
        "bin;dmg;jve;nsv;med;mod;7z;iso;lwtp;pmf;m4a;bz2;sea;pf;arc;arj;"+
        "bz;tbz;mime;taz;ua;toast;lit;rpm;deb;pkg;sxw;l6t;srt;sub;idx;mkv;"+
        "ogm;shn;dvi;rmvp;kar;cdg;ccd;cue;c;h;m;java;jar;pl;py;pyc;"+
        "pyo;pyz;";

    /**
     * If to not force disable sensitive extensions.
     */
    public static final BooleanSetting DISABLE_SENSITIVE =
        FACTORY.createBooleanSetting("DISABLE_SENSITIVE_EXTS", true);
    /**
     * Used to flag the first use of the new database type to migrate the 
     *  extensions database across into the new settings 
     */
    public static final BooleanSetting EXTENSIONS_MIGRATE = 
        FACTORY.createBooleanSetting("EXTENSIONS_MIGRATE", true);
    /**
     * The list of extensions disabled by default in the file types sharing screen
     */
    public static final String[] getDefaultDisabledExtensions() {
        return StringArraySetting.decode(OldLibrarySettings.DEFAULT_EXTENSIONS_TO_DISABLE); 
    }
    /**
     * The list of extensions shared by default
     */
    public static final String getDefaultExtensionsAsString() {
        return DEFAULT_EXTENSIONS_TO_SHARE;
    }
    /**
     * The list of extensions shared by default
     */
    public static final String[] getDefaultExtensions() {
        return StringArraySetting.decode(DEFAULT_EXTENSIONS_TO_SHARE); 
    }
    /**
     * Default disabled extensions.
     */
    private static final String DEFAULT_EXTENSIONS_TO_DISABLE =
        "doc;pdf;xls;rtf;bak;csv;dat;docx;xlsx;xlam;xltx;xltm;xlsm;xlsb;dotm;docm;dotx;dot;qdf;qtx;qph;qel;qdb;qsd;qif;mbf;mny";
    /**
     * The list of extensions disabled by default in the file types sharing screen
     */
    public static final String getDefaultDisabledExtensionsAsString() {
        return DEFAULT_EXTENSIONS_TO_DISABLE; 
    }
    
    /** The shared directories. */
    public static final FileSetSetting DIRECTORIES_TO_SHARE =
        FACTORY.createFileSetSetting("DIRECTORIES_TO_SEARCH_FOR_FILES", new File[0]);
    /**
     * File extensions that are shared.
     */
    public static final StringSetting EXTENSIONS_TO_SHARE =
        FACTORY.createStringSetting("EXTENSIONS_TO_SEARCH_FOR", DEFAULT_EXTENSIONS_TO_SHARE);
    /**
     * List of Extra file extensions.
     */
    public static final StringSetting EXTENSIONS_LIST_CUSTOM =
         FACTORY.createStringSetting("EXTENSIONS_LIST_CUSTOM", "");
    /**
     * File extensions that are not shared.
     */
    public static final StringSetting EXTENSIONS_LIST_UNSHARED =
         FACTORY.createStringSetting("EXTENSIONS_LIST_UNSHARED", "");
    
    /**
     * Directory for saving songs purchased from LimeWire Store (LWS).
     */
    public static final FileSetting DIRECTORY_FOR_SAVING_LWS_FILES = FACTORY.createFileSetting(
            "DIRETORY_FOR_SAVING_LWS_FILES", SharingSettings.DEFAULT_SAVE_LWS_DIR).setAlwaysSave(true);
}
