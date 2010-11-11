package com.limegroup.gnutella.downloader.serial;

import java.net.URI;
import java.util.List;


/**
 * Defines an interface from which bittorrent meta-info can be saved and recreated over
 * different sessions.
 */
public interface BTMetaInfoMemento {

    List<byte[]> getHashes();

    void setHashes(List<byte[]> hashes);

    int getPieceLength();

    void setPieceLength(int pieceLength);

    TorrentFileSystemMemento getFileSystem();

    void setFileSystem(TorrentFileSystemMemento fileSystem);

    byte[] getInfoHash();

    void setInfoHash(byte[] infoHash);

    float getRatio();

    void setRatio(float ratio);

    BTDiskManagerMemento getFolderData();

    void setFolderData(BTDiskManagerMemento folderData);

    URI[] getTrackers();

    void setTrackers(URI[] trackers);

    boolean isPrivate();

    void setPrivate(boolean aPrivate);
    
    URI[] getWebSeeds() ;
    
    void setWebSeeds(URI[] webSeeds);
    
    
}
