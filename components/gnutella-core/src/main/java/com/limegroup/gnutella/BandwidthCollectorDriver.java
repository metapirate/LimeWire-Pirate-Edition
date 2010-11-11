package com.limegroup.gnutella;

import org.limewire.core.api.network.BandwidthCollector;

/**
 * Provides a way to force a new bandwidth collection. 
 */
public interface BandwidthCollectorDriver extends BandwidthCollector {
    /**
     * Causes the Bandwidth collector to collect stats again.
     */
    public void collectBandwidthData();
}
