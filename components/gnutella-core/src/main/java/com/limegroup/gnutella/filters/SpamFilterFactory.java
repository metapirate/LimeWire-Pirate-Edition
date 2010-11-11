package com.limegroup.gnutella.filters;

public interface SpamFilterFactory {

    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets I display in
     * search results.
     */
    public SpamFilter createPersonalFilter();

    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets to route.
     */
    public SpamFilter createRouteFilter();

}