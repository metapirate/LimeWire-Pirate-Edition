package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;
import org.limewire.util.OSUtils;

/**
 * Settings for LimeWire application.
 */
public class ApplicationSettings extends LimeProps {

    private ApplicationSettings() {}
    
    /**
     * The Client ID number.
     */
    public static final StringSetting CLIENT_ID = 
        FACTORY.createStringSetting("CLIENT_ID", "");

    /**
     * The average time this user leaves the application running.
     */
    public static final LongSetting AVERAGE_UPTIME =
        FACTORY.createExpirableLongSetting("AVERAGE_UPTIME", 0);
    
    /** The length of the last n sessions, in seconds. */
    public static final StringArraySetting UPTIME_HISTORY =
        FACTORY.createStringArraySetting("UPTIME_HISTORY", new String[0]);
   
    /** The length of the last n intervals between sessions, in seconds. */
    public static final StringArraySetting DOWNTIME_HISTORY =
        FACTORY.createStringArraySetting("DOWNTIME_HISTORY", new String[0]);
    
    /**
	 * The total time this user has used the application.
	 */    
    public static final LongSetting TOTAL_UPTIME =
        FACTORY.createLongSetting("TOTAL_UPTIME", 0);
    
    /**
     * The average time this user is connected to the network per session (in seconds).
     */        
    public static final LongSetting AVERAGE_CONNECTION_TIME =
        FACTORY.createExpirableLongSetting("AVERAGE_CONNECTION_TIME", 0L);
    
    /**
     * The total time this user has been connected to the network (in seconds).
     */    
    public static final LongSetting TOTAL_CONNECTION_TIME =
        FACTORY.createLongSetting("TOTAL_CONNECTION_TIME", 0L);
    
    /**
     * The total number of times this user has connected-disconnected from the network.
     */    
    public static final IntSetting TOTAL_CONNECTIONS =
        FACTORY.createIntSetting("TOTAL_CONNECTIONS", 0);
    
    /**
     * The total number of times the application has been run --
	 * used in calculating the average amount of time this user
	 * leaves the application on. Initialized to 0 because it will be
     * incremented at the start of each session, after loading settings.
     */
    public static final IntSetting SESSIONS =
        FACTORY.createIntSetting("SESSIONS", 0);
    
    /**
     * The time when the program was last shut down (system time in
     * milliseconds). This value is periodically updated while running in case
     * the program shuts down unexpectedly, so it's only at startup that it
     * represents the previous session's shutdown time. 
     */
    public static final LongSetting LAST_SHUTDOWN_TIME =
        FACTORY.createLongSetting("LAST_SHUTDOWN_TIME", 0);
    
    /**
     * Whether the last shutdown was graceful or not.
     */
    public static final BooleanSetting PREVIOUS_SHUTDOWN_WAS_GRACEFUL =
        FACTORY.createBooleanSetting("PREVIOUS_SHUTDOWN_WAS_GRACEFUL", true);
    
    /**
     * Indicates whether an instance of LimeWire is running or not. 
     * Should always be false at the start up if LimeWire was shutdown properly.
     * It will get set to true once the core is started.
     */
    public static final BooleanSetting CURRENTLY_RUNNING =
        FACTORY.createBooleanSetting("CURRENTLY_RUNNING", false);            
    
    /**
     * The fraction of time the program is running, a unitless quality. This is
     * used to identify highly available hosts.
     */    
    public static final FloatSetting FRACTIONAL_UPTIME =
        FACTORY.createFloatSetting("FRACTIONAL_UPTIME", 0.0f);
    
    /**
	 * The language to use for the application.
	 */
    public static final StringSetting LANGUAGE =
        FACTORY.createStringSetting("LANGUAGE", 
            System.getProperty("user.language", ""));
    
    /**
	 * The country to use for the application.
	 */
    public static final StringSetting COUNTRY =
        FACTORY.createStringSetting("COUNTRY", 
            System.getProperty("user.country", ""));
    
    /**
	 * The locale variant to use for the application.
	 */
    public static final StringSetting LOCALE_VARIANT =
        FACTORY.createStringSetting("LOCALE_VARIANT", 
            System.getProperty("user.variant", ""));
   
                
    /**
     * Setting for whether or not to create an additional manual GC thread.
     */
    public static final BooleanSetting AUTOMATIC_MANUAL_GC =
        FACTORY.createBooleanSetting("AUTOMATIC_MANUAL_GC", OSUtils.isMacOSX());

    /**
     * the default locale to use if not specified
     * used to set the locale for connections which don't have X_LOCALE_PREF
     * header or pings and pongs that don't advertise locale preferences.
     */
    public static final StringSetting DEFAULT_LOCALE = 
        FACTORY.createStringSetting("DEFAULT_LOCALE", "en");
        

    /**
     * Whether or not to use 'secure results' to screen search results.
     */
    public static final BooleanSetting USE_SECURE_RESULTS =
        FACTORY.createBooleanSetting("USE_SECURE_RESULTS", true);
    
    /**
     * Returns true if local access to the REST API is enabled.
     */
    public static final BooleanSetting LOCAL_REST_ACCESS_ENABLED =
        FACTORY.createBooleanSetting("LOCAL_REST_ACCESS_ENABLED", false);
    
    /**
     * Gets the current language setting.
     */
    public static String getLanguage() {
        String lc = LANGUAGE.get();
        String cc = COUNTRY.get();
        String lv = LOCALE_VARIANT.get();
        String lang = lc;
        if(cc != null && !cc.equals(""))
            lang += "_" + cc;
        if(lv != null && !lv.equals(""))
            lang += "_" + lv;
        return lang;
    }
}
