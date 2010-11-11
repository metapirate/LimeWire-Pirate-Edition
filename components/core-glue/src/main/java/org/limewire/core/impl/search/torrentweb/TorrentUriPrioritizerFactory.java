package org.limewire.core.impl.search.torrentweb;

import java.net.URI;

/**
 * Creates a torrent uri prioritizer for a given query and referring
 * uri.
 */
public interface TorrentUriPrioritizerFactory {
    TorrentUriPrioritizer create(String query, URI referrer);
}
