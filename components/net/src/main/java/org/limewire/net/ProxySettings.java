package org.limewire.net;

/** All settings that control how a proxy should be established. */
public interface ProxySettings {
    
    enum ProxyType {
        NONE, SOCKS4, SOCKS5, HTTP;
    }
    
    /** Returns the current type of proxy that should be used for connections. */
    public ProxyType getCurrentProxyType();
    
    /** Determines if private connections should be proxied. */
    public boolean isProxyForPrivateEnabled();
    
    /** Returns true if the proxy should be authenticated. */
    public boolean isProxyAuthenticationRequired();
    
    /** Returns the username, if any, for the proxy. */
    public String getProxyUsername();
    
    /** Returns the password, if any, for the proxy. */
    public String getProxyPassword();

    /** Returns the port of the proxy host. */
    public int getProxyPort();

    /** Returns the address of the proxy host. */
    public String getProxyHost();

}
