package org.limewire.core.settings;

import java.util.Properties;

import org.limewire.setting.PropertiesSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for geo coding the client.
 */
public class GeocodeSettings extends LimeProps {
    
    private GeocodeSettings() {}
                                                        
	/**
     * The URL used to geocode the client.
     */
    static final StringSetting GEOCODE_URL =
        FACTORY.createRemoteStringSetting("GEOCODE_URL", "http://geo.links.limewire.com/geo/");    
    
    /** Last recorded geo location. */
    public static final PropertiesSetting GEO_LOCATION =
        FACTORY.createPropertiesSetting("GEO_LOCATION", new Properties());

}
