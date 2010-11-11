package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/**
 * Settings related to URNs
 */
public class URNSettings extends LimeProps {
    
    private URNSettings() {}
    
    /**
	 * Setting to generate and use Non-metadata hashes. If true, non-metadata hases
 	 * will be created, if false they will not be created.
	 */
    public static final BooleanSetting USE_NON_METADATA_HASH = FACTORY.createRemoteBooleanSetting(
            "USE_NON_METADATA_HASH", true);

}
