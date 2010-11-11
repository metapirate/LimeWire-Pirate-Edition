package org.limewire.libtorrent;

import java.util.Arrays;
import java.util.List;

import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentTracker;

class TorrentInfoImpl implements TorrentInfo {
    private final List<TorrentFileEntry> fileEntries;

    private final LibTorrentInfo libTorrentInfo;

    public TorrentInfoImpl(LibTorrentInfo libTorrentInfo, TorrentFileEntry[] fileEntries) {
        this.fileEntries = Arrays.asList(fileEntries);
        this.libTorrentInfo = libTorrentInfo;
    }

    @Override
    public List<TorrentFileEntry> getTorrentFileEntries() {
        return fileEntries;
    }

    @Override
    public int getPieceLength() {
        return libTorrentInfo.piece_length;
    }

    @Override
    public List<TorrentTracker> getTrackers() {
        return libTorrentInfo.getTrackers();
    }

    @Override
    public List<String> getSeeds() {
        return libTorrentInfo.getSeeds();
    }

    @Override
    public String getName() {
        return libTorrentInfo.name.toString();
    }
}
