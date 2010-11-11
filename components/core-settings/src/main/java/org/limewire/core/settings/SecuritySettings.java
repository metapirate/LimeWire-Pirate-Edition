package org.limewire.core.settings;

import org.limewire.setting.LongSetting;
import org.limewire.setting.StringSetting;

/**
 * Security Settings.
 */
public final class SecuritySettings extends LimeProps {
    
    private SecuritySettings() {}
    
    /**
     * The interval in which a new KeyGenerator(s) will be created.
     */
    public static final LongSetting CHANGE_QK_EVERY
        = FACTORY.createRemoteLongSetting("CHANGE_QK_EVERY", 
                6L*60L*60L*1000L);
    
    /**
     * The grace period for which an old QK stays valid.
     */
    public static final LongSetting QK_GRACE_PERIOD
        = FACTORY.createRemoteLongSetting("QK_GRACE_PERIOD", 
                20L*60L*1000L);
    /**
     * The local node's private identity information
     */
    public static final StringSetting SECURE_IDENTITY
        = FACTORY.createStringSetting("SECURE_IDENTITY", "");
}