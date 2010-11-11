package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.SettingsFactory;
import org.limewire.setting.StringSetSetting;
import org.limewire.setting.StringSetting;

/**
 * Handles installation preferences.
 */
public final class InstallSettings extends LimeWireSettings {

    private static final InstallSettings INSTANCE =
        new InstallSettings();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    public static InstallSettings instance() {
        return INSTANCE;
    }

    private InstallSettings() {
        super("installation.props", "LimeWire installs file");
    }
    
    /**
     * Whether or not the 'Scan for files' question has been asked.
     */
    public static final BooleanSetting SCAN_FILES =
        FACTORY.createBooleanSetting("SCAN_FILES", false);
        
    /**
     * Whether or not the 'Start on startup' question has been asked.
     */
    public static final BooleanSetting START_STARTUP =
        FACTORY.createBooleanSetting("START_STARTUP", false);
        
    /**
     * Whether or not the 'Choose your language' question has been asked.
     */
    public static final BooleanSetting LANGUAGE_CHOICE =
        FACTORY.createBooleanSetting("LANGUAGE_CHOICE", false);
        
    /**
     * Whether or not the firewall warning question has been asked.
     */
    public static final BooleanSetting FIREWALL_WARNING =
        FACTORY.createBooleanSetting("FIREWALL_WARNING", false);
    
    /** Whether Auto-Sharing question has been asked. */
    public static final BooleanSetting AUTO_SHARING_OPTION =
        FACTORY.createBooleanSetting("AUTO_SHARING_OPTION", false);
    
    /** Whether the association option has been asked. */
    public static final IntSetting ASSOCIATION_OPTION =
    	FACTORY.createIntSetting("ASSOCIATION_OPTION", 0);

    /** Whether the association option has been asked. */
    public static final BooleanSetting EXTENSION_OPTION =
        FACTORY.createBooleanSetting("EXTENSION_OPTION", false);
    
    /** Whether the setup wizard has been completed on 5. */
    public static final BooleanSetting UPGRADED_TO_5 =
        FACTORY.createBooleanSetting("UPGRADED_TO_5", false);
        
    /**
     * Stores the value of the last known version of limewire that has been run. Will be null on a clean install until the program is run and a value is set for it.
     * This setting starts with versions > 5.2.2 
     */
    public static final StringSetting LAST_VERSION_RUN = FACTORY.createStringSetting("LAST_VERSION_RUN", "");
    
    /**
     * Stores the java version that was used to run the last known version of limewire. It msut be read early enough, or it will be overwritten with the current value.
     */
    public static final StringSetting LAST_JAVA_VERSION_RUN = FACTORY.createStringSetting("LAST_JAVA_VERSION_RUN", "");
    
    /**
     * Stores an array of all the known versions of limewire that have been run.
     * This setting starts with versions > 5.2.2
     */
    public static final StringSetSetting PREVIOUS_RAN_VERSIONS = FACTORY.createStringSetSetting("PREVIOUS_RAN_VERSIONS", "");
    
}
