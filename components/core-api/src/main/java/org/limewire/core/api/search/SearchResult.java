package org.limewire.core.api.search;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;

public interface SearchResult {

    String getFileExtension();
    
    Object getProperty(FilePropertyKey key);
    
    Category getCategory();

    long getSize();
    
    /** Returns the source of the search result. */
    RemoteHost getSource();
    
    URN getUrn();
    
    public boolean isSpam();
    
    /**
     * @return full file name including extension
     */
    String getFileName();
    
    /** Returns the filename without an extension. */
    String getFileNameWithoutExtension();
    
    String getMagnetURL();

    /**
     * Returns a score that indicates the quality of the sources and the degree
     * to which the result matches the query. Non-anonymous sources with active
     * capabilities (ie. browseable) are given greatest weight.
     */
    public float getRelevance(String query);
    
    /**
     * @return true if the underlying associated file contains a license
     */
    boolean isLicensed();
}