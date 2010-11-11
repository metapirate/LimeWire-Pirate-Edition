package com.limegroup.bittorrent;

import org.limewire.bittorrent.TorrentPeer;
import org.limewire.core.api.transfer.SourceInfo;

class TorrentSourceInfoAdapter implements SourceInfo {
    private final TorrentPeer source;

    public TorrentSourceInfoAdapter(TorrentPeer source) {
        this.source = source;
    }

    @Override
    public String getClientName() {
        return source.getClientName();
    }

    @Override
    public float getDownloadSpeed() {
        return source.getDownloadSpeed();
    }

    @Override
    public String getIPAddress() {
        return source.getIPAddress();
    }

    @Override
    public float getUploadSpeed() {
        return source.getUploadSpeed();
    }

    @Override
    public boolean isEncyrpted() {
        return source.isEncrypted();
    }
}