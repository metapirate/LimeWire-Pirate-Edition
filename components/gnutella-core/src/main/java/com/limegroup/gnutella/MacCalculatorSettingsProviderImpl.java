package com.limegroup.gnutella;

import org.limewire.core.settings.SecuritySettings;
import org.limewire.security.SettingsProvider;


public class MacCalculatorSettingsProviderImpl implements SettingsProvider {
    public long getChangePeriod() {
        return SecuritySettings.CHANGE_QK_EVERY.getValue();
    }

    public long getGracePeriod() {
        return SecuritySettings.QK_GRACE_PERIOD.getValue();
    }
}
