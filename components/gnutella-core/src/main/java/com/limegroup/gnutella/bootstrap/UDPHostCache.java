package com.limegroup.gnutella.bootstrap;

import java.io.IOException;
import java.io.Writer;

import com.limegroup.gnutella.ExtendedEndpoint;

/**
 * Defines an interface for managing a set of UDP host caches and retrieving
 * hosts from them.
 */
public interface UDPHostCache {
    /**
     * Writes the set of UHCs to the given stream.
     */
    void write(Writer out) throws IOException;

    /**
     * Returns true if the set of UHCs needs to be saved.
     */
    boolean isWriteDirty();

    /**
     * Returns the number of UHCs in the set.
     */
    int getSize();

    /**
     * Attempts to contact some UHCs to retrieve hosts.
     */
    boolean fetchHosts();

    /**
     * Adds a new UHC to the set, returning true if it was added.
     */
    boolean add(ExtendedEndpoint e);
}
