package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.Set;

/**
 * Persistent store for information about torrent uris.
 */
public interface TorrentUriStore {
    
    /**
     * @return true if the exact uri points to a torrent
     */
    boolean isTorrentUri(URI uri);
    /**
     * @return true if it is known that this uri does not point to a torrent 
     */
    boolean isNotTorrentUri(URI uri);
    /**
     * Marks <code>uri</code> as torrent or non-torrent. 
     */
    void setIsTorrentUri(URI uri, boolean isTorrent);
    /**
     * @return set of canoncicalized torrent uris for <code>host</code> or empty
     * set if there are none
     */
    Set<URI> getTorrentUrisForHost(String host);
    /**
     * Adds <code>uri</code> to the set of canonicalized torrent uris
     * for <code>host</code>
     */
    void addCanonicalTorrentUri(String host, URI uri);
    
}
