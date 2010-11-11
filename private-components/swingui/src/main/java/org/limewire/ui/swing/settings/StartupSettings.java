package org.limewire.ui.swing.settings; 

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.BooleanSetting;

/** 
 * Settings for starting limewire. 
 */ 
public final class StartupSettings extends LimeProps {
    
    private StartupSettings() {}
    
    /** 
     * Setting for whether or not to allow multiple instances of LimeWire. 
     */ 
    public static final BooleanSetting ALLOW_MULTIPLE_INSTANCES = 
        FACTORY.createBooleanSetting("ALLOW_MULTIPLE_INSTANCES", false); 
        
    /**
     * A boolean flag for whether or not to start LimeWire on system startup.
     */
    public static final BooleanSetting RUN_ON_STARTUP = 
        FACTORY.createBooleanSetting("RUN_ON_STARTUP", true);
    
    /**
     * Whether or not tips should be displayed on startup.
     */
    public static final BooleanSetting SHOW_TOTD =
    	FACTORY.createBooleanSetting("SHOW_TOTD", true);
}
