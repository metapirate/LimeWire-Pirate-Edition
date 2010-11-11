package org.limewire.ui.swing.friends.settings;

import java.util.List;

/**
 * Loads and manages XMPP account settings.
 */
public interface FriendAccountConfigurationManager {

    /**
     * Returns the account configuration associated with the specified label,
     * or null if there is no such configuration.
     */
    public FriendAccountConfiguration getConfig(String label);

    /**
     * Returns the labels of all known account configurations in alphabetical
     * order.
     */
    public List<String> getLabels();
    
    /**
     * Returns all configurations.
     */
    public List<FriendAccountConfiguration> getConfigurations();    

    /**
     * Returns the account configuration that should log in automatically when
     * LimeWire starts, or null if auto-login is disabled.
     */
    public FriendAccountConfiguration getAutoLoginConfig();

    /**
     * Sets the account configuration that should log in automatically when
     * LimeWire starts; use null to disable auto-login.
     */
    public void setAutoLoginConfig(FriendAccountConfiguration config);
}
