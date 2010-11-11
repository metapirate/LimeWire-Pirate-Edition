package com.limegroup.gnutella.bootstrap;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ExtendedEndpoint;

public interface Bootstrapper extends Runnable {

    /** Resets the bootstrapper to its initial state. */
    void reset();

    /** Adds a UHC to the bootstrapper, returning true if the UHC was added. */
    boolean addUDPHostCache(ExtendedEndpoint ee);

    /** Returns true if the bootstrapper needs to save its list of UHCs. */
    boolean isWriteDirty();

    /** Writes the bootstrapper's list of UHCs to the given writer. */
    void write(Writer out) throws IOException;

    static interface Listener {
        /** Returns true if the listener needs hosts from the bootstrapper. */
        boolean needsHosts();

        /** Receives hosts from the bootstrapper, returning the number used. */
        int handleHosts(Collection<? extends Endpoint> hosts);
    }
}
