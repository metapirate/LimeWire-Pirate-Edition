package com.limegroup.gnutella.dht.db;

import org.limewire.io.GUID;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;

/**
 * An AltLocFinder finds alternate locations for a urn or retrieves the 
 * alternate location for a {@link GUID}.
 */
// TODO this interface could be moved outside of the DHT component, it doesn't have to be the DHT
// that does the lookup
public interface AltLocFinder {

    /**
     * Finds alternate locations for the given URN.
     * 
     * @param urn for the alternate location, must not be null
     */
    Shutdownable findAltLocs(URN urn, SearchListener<AlternateLocation> listener);

}