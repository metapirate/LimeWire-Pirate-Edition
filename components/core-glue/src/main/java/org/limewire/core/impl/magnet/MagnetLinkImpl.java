package org.limewire.core.impl.magnet;

import java.net.URI;
import java.util.List;

import org.limewire.core.api.magnet.MagnetLink;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;

public class MagnetLinkImpl implements MagnetLink {
    
    private final MagnetOptions magnetOptions;
    
    public MagnetLinkImpl(MagnetOptions magnetOptions) {
        this.magnetOptions = magnetOptions;
    }

    @Override
    public boolean isGnutellaDownloadable() {
        return magnetOptions.isGnutellaDownloadable();
    }
    
    @Override
    public boolean isTorrentDownloadable() {
        return magnetOptions.isTorrentDownloadable();
    }

    @Override
    public boolean isKeywordTopicOnly() {
        return magnetOptions.isKeywordTopicOnly();
    }
    
    public MagnetOptions getMagnetOptions() {
        return magnetOptions;
    }
    
    @Override
    public String getQueryString() {
        return magnetOptions.getQueryString();
    }
    
    @Override
    public URN getURN() {
        return magnetOptions.getSHA1Urn();
    }
    
    @Override
    public List<URI> getTrackerUrls() {
         return magnetOptions.getTrackers();
    }
    
    @Override
    public String getName() {
        String name = magnetOptions.getDisplayName();
        if (name == null) {
            name = magnetOptions.getFileNameForSaving();
        }
        
        return sanatiseName(name);
    }
    
    /**
     * Strip any strange characters out of the name so it is appropriate for 
     *  naming files, etc. on all systems. 
     */
    private static String sanatiseName(String name) {
        // sha1 is the only offending name possible so far
        //  replace it with the word magnet for now
        if (name.startsWith("urn:sha1:")) {
            return "Magnet " + name.substring(9);
        }
        return name;
    }
}
