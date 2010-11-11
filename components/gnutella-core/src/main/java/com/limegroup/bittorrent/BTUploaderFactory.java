package com.limegroup.bittorrent;

import org.limewire.bittorrent.Torrent;

public interface BTUploaderFactory {

    public abstract BTUploader createBTUploader(Torrent torrent);

}