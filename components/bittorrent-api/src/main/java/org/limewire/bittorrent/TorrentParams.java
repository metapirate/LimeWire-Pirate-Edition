package org.limewire.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Parameters used to create a torrent. Currently sha1 downloadFolder and name
 * are required field. The other fields are optional.
 */
public interface TorrentParams {

    public String getName();

    public void setName(String name);

    public String getSha1();

    public void setSha1(String sha1);

    public List<URI> getTrackers();

    public void setTrackers(List<URI> trackers);

    public File getFastResumeFile();

    public void setFastResumeFile(File fastResumeFile);

    public File getTorrentFile();

    public File getDownloadFolder();

    public void setTorrentFile(File torrentFile);

    public File getTorrentDataFile();

    public void setTorrentDataFile(File torrentDataFile);

    public Boolean getPrivate();

    public void setPrivate(Boolean isPrivate);

    public void setSeedRatioLimit(float value);
    
    public float getSeedRatioLimit();
    
    public void setTimeRatioLimit(int value);
    
    public int getTimeRatioLimit();
    
    /**
     * Fills in missing fields from the data in the torrentFile field if it
     * exists and is valid.
     */
    public void fill() throws IOException;
}
