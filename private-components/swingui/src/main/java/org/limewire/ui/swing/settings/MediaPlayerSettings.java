package org.limewire.ui.swing.settings;

import java.util.Properties;

import org.limewire.setting.PropertiesSetting;
import org.limewire.setting.SettingsFactory;
import org.limewire.core.settings.LimeWireSettings;

/**
 * LimeWire Media Player settings.  Used to persist cumulative statistics
 * over many sessions, which are collected via inspection.
 */
public class MediaPlayerSettings extends LimeWireSettings {
    
    private static final MediaPlayerSettings INSTANCE = new MediaPlayerSettings();
    private static final SettingsFactory FACTORY = INSTANCE.getFactory();

    private MediaPlayerSettings() {
        super("player.props", "LimeWire Media Player Settings");
    }
    
    /**
     * Percentage of a file that has been played.
     * Measured when user stops playing a file.
     * 
     * Key   --> Percentage
     * Value --> How many times this percentage of a file has been played
     */
    public static final PropertiesSetting MEDIA_PLAYER_PERCENT_PLAYED = 
        FACTORY.createPropertiesSetting("MEDIA_PLAYER_PERCENT_PLAYED", new Properties());

    /**
     * Number of times each file has been played.  
     * Also used to calculate repeat plays.
     * 
     * Key   --> File Name
     * Value --> Number of times the file has been played
     */
    public static final PropertiesSetting MEDIA_PLAYER_NUM_PLAYS =
        FACTORY.createPropertiesSetting("MEDIA_PLAYER_NUM_PLAYS", new Properties());

    /**
     * Quicklist sizes and how often they are played.
     * 
     * Key   --> Size of the quick list being played
     * Value --> How many times each list of that size is played
     */
    public static final PropertiesSetting MEDIA_PLAYER_LIST_SIZE =
        FACTORY.createPropertiesSetting("MEDIA_PLAYER_LIST_SIZE", new Properties()); 
}
