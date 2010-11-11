package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;

/**
 * Settings for Ultrapeers.
 */
public final class UltrapeerSettings extends LimeProps {
    
    private UltrapeerSettings() {}

	/**
	 * Setting for whether or not we've ever been Ultrapeer capable.
	 */
	public static final BooleanSetting EVER_ULTRAPEER_CAPABLE =
		FACTORY.createExpirableBooleanSetting("EVER_SUPERNODE_CAPABLE", false);


	/**
	 * Setting for whether or not to force Ultrapeer mode.
	 */
	public static final BooleanSetting FORCE_ULTRAPEER_MODE =
		FACTORY.createBooleanSetting("FORCE_SUPERNODE_MODE", false);

	/**
	 * Setting for whether or not to disable Ultrapeer mode.
	 */
	public static final BooleanSetting DISABLE_ULTRAPEER_MODE =
		FACTORY.createBooleanSetting("DISABLE_SUPERNODE_MODE", false);

	
	/**
	 * Setting for the maximum leaf connections.
	 */
	public static final IntSetting MAX_LEAVES =
		FACTORY.createIntSetting("MAX_LEAVES_2", 40);
    
    /**
     * The minimum number of upstream kbytes per second that 
     * a node must be able to transfer in order to qualify as a ultrapeer.
     */
    public static final IntSetting MIN_UPSTREAM_REQUIRED =
        FACTORY.createIntSetting("MIN_UPSTREAM_REQUIRED_2", 10);
    
    /**
     * The minimum number of downlstream kbytes per second that 
     * a node must be able to transfer in order to qualify as a ultrapeer.
     */
    public static final IntSetting MIN_DOWNSTREAM_REQUIRED =
        FACTORY.createIntSetting("MIN_DOWNSTREAM_REQUIRED_2", 20);
    
    /**
     * The minimum average uptime in seconds that a node must have to qualify for ultrapeer status.
     */
    public static final IntSetting MIN_AVG_UPTIME =
        FACTORY.createIntSetting("MIN_AVG_UPTIME_2", 3600);
    
    /**
     * Setting for whether or not the MIN_CONNECT_TIME is required.
     */
    public static final BooleanSetting NEED_MIN_CONNECT_TIME = 
        FACTORY.createBooleanSetting("NEED_MIN_CONNECT_TIME", true);
    
    /**
     * The minimum time in seconds that a node must have tried to connect before it can 
     * qualify for Ultrapeer status.
     */
    public static final IntSetting MIN_CONNECT_TIME =
        FACTORY.createIntSetting("MIN_CONNECT_TIME_2", 4);
    
    /**
     * The minimum current uptime in seconds that a node must have to qualify for Ultrapeer status.
     */
    public static final IntSetting MIN_INITIAL_UPTIME =
        FACTORY.createIntSetting("MIN_INITIAL_UPTIME_2", 7200);
    
    /**
     * The amount of time to wait between attempts to become an Ultrapeer, in milliseconds.
     */
    public static final IntSetting UP_RETRY_TIME =
        FACTORY.createIntSetting("UP_RETRY_TIME_2", 10800000);
}

