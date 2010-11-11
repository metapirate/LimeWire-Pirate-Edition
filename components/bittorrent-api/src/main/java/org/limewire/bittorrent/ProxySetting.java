package org.limewire.bittorrent;

/**
 * Represents a proxy address, login credentials and type.
 */
public interface ProxySetting {
    /**
     * Returns the hostname for this setting.
     */
    public String getHostname();

    /**
     * Returns the port for this setting.
     */
    public int getPort();

    /**
     * Returns the username to connect to this proxy, May be null.
     */
    public String getUsername();

    /**
     * Contains the password to connect to this proxy. May be null.
     */
    public String getPassword();

    /**
     * Returns the type of this proxy.
     */
    public ProxySettingType getType();
}
