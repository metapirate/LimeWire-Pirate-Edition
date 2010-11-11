package org.limewire.bittorrent;

/**
 * Represents a file in the torrent. 
 */
public interface TorrentFileEntry {
    /**
     * Returns the index of the file in the torrent.
     */
    public int getIndex();

    /**
     * Returns the path of the file in the torrent.
     */
    public String getPath();
    
    /**
     * Returns the size of this file entry in the torrent. 
     */
    public long getSize();
    
    /**
     * Returns the total number of bytes downloaded for this file so far. 
     */
    public long getTotalDone();
    
    /**
     * Returns the download priority for this file.
     */
    public int getPriority();
    
    /**
     * Returns the progress for downloading this file. 
     */
    public float getProgress();
}
