package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentIpPort;

public class LibTorrentIpPort implements TorrentIpPort {

    private final String address;
    private final int port;

    public LibTorrentIpPort(String address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }

}
