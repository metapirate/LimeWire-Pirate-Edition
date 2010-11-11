package org.limewire.core.impl.search.torrentweb;

/**
 * Defines requirements for a class that stores robots.txt entries.
 */
public interface TorrentRobotsTxtStore {

    /**
     * Maximum allowed size of a robots.txt entry.
     */
    public static final int MAX_ROBOTS_TXT_SIZE = 5 * 1024;
    
    /**
     * @return null if no robots.txt exists for <code>host</code> 
     */
    public String getRobotsTxt(String host);
    /**
     * Stores robots text under key <code>host</code>
     * 
     * @throws IllegalArgumentException if <code>robotsTxt</code> is
     * larger than {@link #MAX_ROBOTS_TXT_SIZE}
     */
    public void storeRobotsTxt(String host, String robotsTxt);
    
}
