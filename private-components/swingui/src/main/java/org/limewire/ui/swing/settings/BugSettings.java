package org.limewire.ui.swing.settings;

import java.io.File;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.IntSetting;
import org.limewire.util.CommonUtils;


/**
 * Settings to deal with bugs.
 */ 
public class BugSettings extends LimeProps {

    private BugSettings() {}

    /**
     * Setting for whether or not to show bugs before they are reported.
     */
    public static final BooleanSetting SHOW_BUGS =
        FACTORY.createBooleanSetting("SHOW_BUGS", true);

    /**
     * Setting for whether or not bugs should be logged locally.
     * Developers can easily change this if they wish to see all
     * bugs logged to disk for future review.
     */
    public static final BooleanSetting LOG_BUGS_LOCALLY =
        FACTORY.createBooleanSetting("LOG_BUGS_LOCALLY", false);
        
    /**
     * Setting for the filename of the local bugfile log.
     */
    public static final FileSetting BUG_LOG_FILE =
        FACTORY.createFileSetting("BUG_LOG_FILE",
            new File(CommonUtils.getUserSettingsDir(), "bugs.log"));
            
    /**
     * Setting for the maximum filesize of the buglog.
     */
    public static final IntSetting MAX_BUGFILE_SIZE =
        FACTORY.createIntSetting("MAX_BUGFILE_SIZE", 1024 * 500); // 500k
        
    /**
     * Setting for the file to use when writing bugs (for serialization)
     * to disk.
     */
    public static final FileSetting BUG_INFO_FILE =
        FACTORY.createFileSetting("BUG_INFO_FILE",
            new File(CommonUtils.getUserSettingsDir(), "bugs.data"));
}
