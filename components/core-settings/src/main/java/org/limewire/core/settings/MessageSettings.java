package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.ProbabilisticBooleanSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for messages.
 */
public class MessageSettings extends LimeProps {  
    private MessageSettings() {}
   
    /** 
     * The maximum allowable length of packets.
     */
    public static final IntSetting MAX_LENGTH = 
        FACTORY.createIntSetting("MAX_LENGTH", 65536);
    
    /**
     * Whether to embed a timestamp in the query guids.
     */
    public static final BooleanSetting STAMP_QUERIES =
        FACTORY.createRemoteBooleanSetting("STAMP_QUERIES", false);
    
    /**
     * The latest handled routeable version of the inspection message.
     */
    public static final LongSetting INSPECTION_VERSION = 
        FACTORY.createLongSetting("INSPECTION_VERSION", 0);
    
    /**
     * A custom criteria for evaluating FileDescs.
     */
    public static final StringArraySetting CUSTOM_FD_CRITERIA =
        FACTORY.createStringArraySetting("CUSTOM_FD_CRITERIA_2", new String[] {
                "ups;atUpSet;<;cups;cUpSet;<;OR;NOT;lastup;rftSet;>;AND" });
    
    /**
     * A guid to track.
     */
    public static final StringSetting TRACKING_GUID = 
        FACTORY.createStringSetting("TRACKNG_GUID_2", "");
    
    /**
     * Whether ttroot urns should go in ggep instead of huge.
     */
    public static final BooleanSetting TTROOT_IN_GGEP = 
        FACTORY.createRemoteBooleanSetting("TTROOT_IN_GGEP", true);
    
    /**
     * Whether to send redundant LIME11 and LIME12 messages.
     */
    public static final BooleanSetting OOB_REDUNDANCY =
        FACTORY.createBooleanSetting("OOB_REDUNDANCY_2", true);
    
    /**
     * Whether to add return path in replies.
     */
    public static final BooleanSetting RETURN_PATH_IN_REPLIES = 
        FACTORY.createRemoteBooleanSetting("RETURN_PATH_IN_REPLIES",
                true);
    
    /**
     * Whether to zero the OOB bytes of the guid as described in experiment LWC-1313.
     */
    public static final BooleanSetting GUID_ZERO_EXPERIMENT = 
        FACTORY.createRemoteBooleanSetting("GUID_ZERO_EXPERIMENT", false);
    
    /**
     * Whether ultrapeers should filter queries to leaves based on firewall status.
     * Described in LWC-1309.
     */
    public static final BooleanSetting ULTRAPEER_FIREWALL_FILTERING =
        FACTORY.createRemoteBooleanSetting("ULTRAPEER_FIREWALL_FILTERING",true);
    
    /** 
     * The maximum number of UDP replies to buffer up.  For testing.
     */
    public static final IntSetting MAX_BUFFERED_OOB_REPLIES =
        FACTORY.createIntSetting("MAX_BUFFERED_OOB_REPLIES", 250);

    /**
     * Probabilistic setting if a signed message with same version number as local
     * one should be rerequested from new connections, to verify signatures
     * against each other.
     */
    public static final ProbabilisticBooleanSetting REREQUEST_SIGNED_MESSAGE =
        FACTORY.createRemoteProbabilisticBooleanSetting("REREQUEST_SIGNED_MESSAGE", 0.2f);
}
