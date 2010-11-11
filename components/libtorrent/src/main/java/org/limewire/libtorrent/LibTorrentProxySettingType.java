package org.limewire.libtorrent;
import java.util.HashMap;
import java.util.Map;

import org.limewire.bittorrent.ProxySettingType;

public enum LibTorrentProxySettingType {
    NONE(0), SOCKS4(1), SOCKS5(2), SOCKS5_PW(3), HTTP(4), HTTP_PW(5);

    private static final Map<Integer, LibTorrentProxySettingType> map;

    static {
        Map<Integer, LibTorrentProxySettingType> builder = new HashMap<Integer, LibTorrentProxySettingType>();
        for (LibTorrentProxySettingType state : LibTorrentProxySettingType.values()) {
            builder.put(state.id, state);
        }
        map = builder;
    }

    private final int id;

    private LibTorrentProxySettingType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static LibTorrentProxySettingType forId(int id) {
        return map.get(id);
    }

    public static LibTorrentProxySettingType forProxySettingType(ProxySettingType type) {
        if (type == null) {
            return NONE;
        }

        switch (type) {
        case SOCKS4:
            return SOCKS4;
        case SOCKS5:
            return SOCKS5;
        case SOCKS5_PW:
            return SOCKS5_PW;
        case HTTP:
            return HTTP;
        case HTTP_PW:
            return HTTP_PW;
        default:
            throw new IllegalArgumentException("Unsupported ProxySettingType: " + type);
        }
    }

    public ProxySettingType getProxySettingType() {
        switch (this) {
        case NONE:
            return null;
        case SOCKS4:
            return ProxySettingType.SOCKS4;
        case SOCKS5:
            return ProxySettingType.SOCKS5;
        case SOCKS5_PW:
            return ProxySettingType.SOCKS5_PW;
        case HTTP:
            return ProxySettingType.HTTP;
        case HTTP_PW:
            return ProxySettingType.HTTP_PW;
        default:
            throw new IllegalArgumentException("Unsupported LibtorrentProxySettingType: " + this);
        }

    }
}
