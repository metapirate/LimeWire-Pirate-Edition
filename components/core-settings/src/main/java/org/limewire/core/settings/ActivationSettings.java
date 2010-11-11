package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for handling Activation.
 */
public class ActivationSettings extends LimeProps {

    private ActivationSettings() {}

    /**
     * Returns whether this was considered a PRO version at the last shutdown. This is
     * needed for starting up some UI components before ActivationManager can be fully
     * initialized.
     */
    public static final BooleanSetting LAST_START_WAS_PRO = FACTORY.createBooleanSetting(
            "LAST_START_WAS_PRO", false);

    /**
     * License Key for activation. If not License Key exists will return empty String.
     */
    public static final StringSetting ACTIVATION_KEY = FACTORY.createStringSetting(
            "ACTIVATION_KEY", "");
    
    /**
     * Saved mcode from the last json String.
     */
    public static final StringSetting MODULE_CODE = FACTORY.createStringSetting(
            "MODULE_CODE", "");
}
