package org.limewire.ui.swing.settings;

import org.limewire.core.settings.LimeWireSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.SettingsFactory;



/**
 * Controls all 'Do not ask this again' or 'Always use this answer' questions.
 */
public class QuestionsHandler extends LimeWireSettings {

    private static final QuestionsHandler INSTANCE =
        new QuestionsHandler();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    private QuestionsHandler() {
        super("questions.props", "LimeWire questions file");
    }

    public static QuestionsHandler instance() {
        return INSTANCE;
    }

	
    /**
     * Initial warning for first download.
     */
    public static final IntSetting SKIP_FIRST_DOWNLOAD_WARNING =
        FACTORY.createIntSetting("SHOW_FIRST_DOWNLOAD_WARNING", 0);
    
    
    
    /**
     * Setting for whether or not to display a message that the user
     * should let a seeding torrent reach 1:1 ratio. If true,
     * display a warning dialog, if false, don't display anything
     */
    public static final BooleanSetting WARN_TORRENT_SEED_MORE =
    	FACTORY.createBooleanSetting("TORRENT_SEED_MORE", true);
	

    /** Setting for whether or not to confirm blocking a host */
    public static final BooleanSetting CONFIRM_BLOCK_HOST =
        FACTORY.createBooleanSetting("CONFIRM_BLOCK_HOST", true);
    
    /** Setting for wether to shown the confirmation dialog when
      * unsharing a file from within FileInfo
      */
    public static final BooleanSetting CONFIRM_REMOVE_FILE_INFO_SHARING =
        FACTORY.createBooleanSetting("CONFIRM_REMOVE_FILE_INFO_SHARING", true);
    

}


