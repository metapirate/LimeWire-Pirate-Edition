package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.PasswordSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;
import org.limewire.util.OSUtils;

/**
 * Settings for Digital Audio Access Protocol (DAAP).
 */
public class DaapSettings extends LimeProps {
    
    private DaapSettings() {}
    
    /**
     * Whether or not DAAP should be enabled.
     */
    public static BooleanSetting DAAP_ENABLED =
	    FACTORY.createBooleanSetting("DAAP_ENABLED", true);
	
    
    /**
     * The audio file types supported by DAAP.
     */
    public static StringArraySetting DAAP_SUPPORTED_AUDIO_FILE_TYPES = 
        FACTORY.createStringArraySetting("DAAP_SUPPORTED_AUDIO_FILE_TYPES", 
            new String[]{".mp3", ".m4a", ".wav", ".aif", ".aiff", ".m1a"});
     
    /**
     * The video file types supported by DAAP. Note: MPEG-2 does not
     * work (requires commercial codec)! AVI isn't in the list as QT 
     * doesn't support most of the codecs... 
     */
    public static StringArraySetting DAAP_SUPPORTED_VIDEO_FILE_TYPES = 
        FACTORY.createStringArraySetting("DAAP_SUPPORTED_VIDEO_FILE_TYPES", 
            new String[]{".mov", ".mp4", ".mpg", ".mpeg"});
    
    /**
     * The name of the Library.
     */
    public static StringSetting DAAP_LIBRARY_NAME =
        (StringSetting)FACTORY.createStringSetting("DAAP_LIBRARY_NAME",
                getPossessiveUserName() + " LimeWire Files").
                setPrivate(true);

    /**
     * The maximum number of simultaneous connections. Note: There
     * is an audio stream per connection (i.e. there are actually 
     * DAAP_MAX_CONNECTIONS*2).
     */
    public static IntSetting DAAP_MAX_CONNECTIONS =
        FACTORY.createIntSetting("DAAP_MAX_CONNECTIONS", 5);
        
    /**
     * The port where the DaapServer is running.
     */
    public static IntSetting DAAP_PORT =
        FACTORY.createIntSetting("DAAP_PORT", 5214);

    /**
     * The fully qualified service type name <code>_daap._tcp.local.</code>.
     * You shouldn't change this value as iTunes won't see our DaapServer.
     */
    public static StringSetting DAAP_TYPE_NAME =
        FACTORY.createStringSetting("DAAP_TYPE_NAME", "_daap._tcp.local.");

    /**
     * The name of the Service. I recommend to set this value to the
     * same as <code>DAAP_LIBRARY_NAME</code>.<p>
     * Note: when you're dealing with mDNS then is the actual Service 
     * name <code>DAAP_SERVICE_NAME.getValue() + "." + 
     * DAAP_TYPE_NAME.getValue()</code>
     */
    public static StringSetting DAAP_SERVICE_NAME =
        (StringSetting)FACTORY.createStringSetting("DAAP_SERVICE_NAME",
                getPossessiveUserName() + " LimeWire Files").
                setPrivate(true);

    /**
     * This isn't important.
     */
    public static IntSetting DAAP_WEIGHT 
        = FACTORY.createIntSetting("DAAP_WEIGHT", 0);
    
    /**
     * This isn't important.
     */
    public static IntSetting DAAP_PRIORITY 
        = FACTORY.createIntSetting("DAAP_PRIORITY", 0);

    /**
     * Whether or not an username is required.
     */
    public static BooleanSetting DAAP_REQUIRES_USERNAME =
        FACTORY.createBooleanSetting("DAAP_REQUIRES_USERNAME", false);
    
    /**
     * Whether or not password protection is enabled.
     */
    public static BooleanSetting DAAP_REQUIRES_PASSWORD =
        FACTORY.createBooleanSetting("DAAP_REQUIRES_PASSWORD", false);
    
    /**
     * The DAAP password.
     */
    public static StringSetting DAAP_USERNAME =
        FACTORY.createStringSetting("DAAP_USERNAME", "");
    
    /**
     * The DAAP password.
     */
    public static PasswordSetting DAAP_PASSWORD =
        FACTORY.createPasswordSettingMD5("DAAP_PASSWORD", "");
    
    /**
     * With default JVM settings we start to run out of memory
     * if the Library becomes greater than 16000 Songs (OSX 10.3,
     * JVM 1.4.2_04, G5 with 2.5GB of RAM). Therefore I'm limiting
     * the max size to 10000 Songs.
     */
    public static IntSetting DAAP_MAX_LIBRARY_SIZE =
        FACTORY.createIntSetting("DAAP_MAX_LIBRARY_SIZE", 10000);
    
    public static IntSetting DAAP_BUFFER_SIZE =
        FACTORY.createIntSetting("DAAP_BUFFER_SIZE_2", 2048);
     
    /**
     * Gets the user's name, in possessive format.
     */
    private static String getPossessiveUserName() {
        String name = null;
        
        if (OSUtils.isMacOSX())
            name = System.getProperty("user.fullname", null);
        
        if (name == null)
            name = System.getProperty("user.name", "Unknown");
        
        if(!name.endsWith("s"))
            name += "'s";
        else
            name += "'";
        
        return name;
    }
}
