package org.limewire.core.impl.search.torrentweb;

/**
 * Creaetes torrent web search for a query.
 */
public interface TorrentWebSearchFactory {
    TorrentWebSearch create(String query);
}
