package org.limewire.core.impl.search.torrentweb;

import java.net.URI;

/**
 * Defines requirements for classes that implement a robots.txt look up
 * for uris.
 */
public interface TorrentRobotsTxt {
    /**
     * @return true if the LimeWire user agent is allowed to access <code>uri</code>
     */
    boolean isAllowed(URI uri);
}
