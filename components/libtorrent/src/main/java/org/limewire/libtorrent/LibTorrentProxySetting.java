package org.limewire.libtorrent;

import org.limewire.bittorrent.ProxySetting;
import org.limewire.bittorrent.ProxySettingType;

import com.sun.jna.Structure;

public class LibTorrentProxySetting extends Structure implements ProxySetting {

    public String hostname;

    public int port;

    public String username;

    public String password;

    public int type;

    public LibTorrentProxySetting(LibTorrentProxySettingType type, String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.type = type.getId();
        this.username = "";
        this.password = "";
    }

    public LibTorrentProxySetting(ProxySetting proxy) {
        this.hostname = proxy.getHostname();
        this.port = proxy.getPort();
        this.username = proxy.getUsername();
        this.password = proxy.getPassword();
        this.type = LibTorrentProxySettingType.forProxySettingType(proxy.getType()).getId();
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public ProxySettingType getType() {
        return LibTorrentProxySettingType.forId(type).getProxySettingType();
    }

    public static LibTorrentProxySetting nullProxy() {
        LibTorrentProxySetting setting = new LibTorrentProxySetting(
                LibTorrentProxySettingType.NONE, "", 0);
        return setting;
    }
}
