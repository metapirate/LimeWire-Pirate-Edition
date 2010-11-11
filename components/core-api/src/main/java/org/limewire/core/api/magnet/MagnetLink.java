package org.limewire.core.api.magnet;

import java.net.URI;
import java.util.List;

import org.limewire.core.api.URN;

public interface MagnetLink {

    public boolean isGnutellaDownloadable();
    
    public boolean isTorrentDownloadable();

    public boolean isKeywordTopicOnly();

    public String getQueryString();
    
    public String getName();

    public URN getURN();
    
    public List<URI> getTrackerUrls();
}
