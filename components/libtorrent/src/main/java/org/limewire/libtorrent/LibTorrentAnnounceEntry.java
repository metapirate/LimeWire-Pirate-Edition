package org.limewire.libtorrent;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.bittorrent.TorrentTracker;
import org.limewire.util.URIUtils;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class LibTorrentAnnounceEntry extends Structure implements TorrentTracker {

    public String uri;
    public int tier;

    public LibTorrentAnnounceEntry() {
        super();
    }

    public LibTorrentAnnounceEntry(Pointer p) {
        super(p);
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public URI getURI() {
        try {
            return URIUtils.toURI(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return uri;
    }
}
