package com.limegroup.gnutella.bootstrap;

/**
 * Defines an interface for managing a set of gwebcaches and retrieving hosts
 * from them.
 */
public interface TcpBootstrap {

    /**
     * Attempts to contact a gwebcache to retrieve endpoints.
     */
    public boolean fetchHosts(Bootstrapper.Listener listener);
}
