package org.limewire.core.settings;

import org.limewire.setting.IntSetting;

public class NetworkSettings extends LimeProps {
    
    private NetworkSettings() {}
    
    /**
     * The port to connect on.
     */
    public static final IntSetting PORT =
        FACTORY.createIntSetting("PORT", 6346);

    
    

}
