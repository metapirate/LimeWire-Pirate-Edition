package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/**
 * Settings for pings and pongs, pong caching and such.
 */
public final class PingPongSettings extends LimeProps {
    
    private PingPongSettings() {}

    /**
     * Setting for whether or not pings should be sent for our pong
     * caching scheme -- useful setting for tests.
     */
    public static final BooleanSetting PINGS_ACTIVE =
        FACTORY.createBooleanSetting("PINGS_ACTIVE", true);
}
