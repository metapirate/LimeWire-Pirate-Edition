package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.net.URI;
import java.util.List;

import com.limegroup.gnutella.URN;

public interface LibTorrentBTDownloadMemento extends DownloadMemento {

    public String getName();

    public File getIncompleteFile();
    
    public URN getSha1Urn();

    public List<URI> getTrackers();

    public String getFastResumePath();

    public String getTorrentPath();
    
    public Boolean isPrivate();
    
    public void setName(String name);

    public void setIncompleteFile(File incompleteFile);
    
    public void setSha1Urn(URN sha1Urn);

    public void setTrackers(List<URI> trackers);

    public void setFastResumePath(String data);

    public void setTorrentPath(String torrentPath);

    public void setPrivate(Boolean isPrivate);

}