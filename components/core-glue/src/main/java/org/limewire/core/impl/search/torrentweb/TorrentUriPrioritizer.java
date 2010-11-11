package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.List;

/**
 * Prioritizes a list of uris by their likelihood of being a torrent uri.
 */
public interface TorrentUriPrioritizer {
    /**
     * @return a potentially smaller list of candidates sorted in order
     * of their likelihood of being a torrent uri
     */
    List<URI> prioritize(List<URI> candidates);
    /**
     * Marks a uri as torrent uri or not. Should be called after an http 
     * request confirmed whether a uri points to a torrent or not. This allows
     * the prioritizer to learn and get better at prioritzing 
     */
    void setIsTorrent(URI uri, boolean isTorrent);
}
