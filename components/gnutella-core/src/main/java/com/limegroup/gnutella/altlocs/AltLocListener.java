package com.limegroup.gnutella.altlocs;

/**
 * Listener that is notified of {@link AlternateLocation alternate locations}
 * being added to the {@link AltLocManager}.
 */
public interface AltLocListener {
    
    /**
     * Called when an alternate location is added.
     * <p>
     * Note: This can be called from any thread and the handling code should
     * be non-blocking and execute fast without throwing exceptions.
     * </p> 
     */
    public void locationAdded(AlternateLocation loc);
}
