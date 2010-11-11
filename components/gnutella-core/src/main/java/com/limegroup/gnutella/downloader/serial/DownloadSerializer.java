package com.limegroup.gnutella.downloader.serial;

import java.io.IOException;
import java.util.List;

/**
 * Allows all downloads to be written & read from disk.
 */
public interface DownloadSerializer {
    
    /** Reads all saved downloads from disk. 
     * @throws IOException */
    public List<DownloadMemento> readFromDisk() throws IOException;
    
    /** Writes all mementos to disk. */
    public boolean writeToDisk(List<? extends DownloadMemento> mementos);    
}
