package org.limewire.nio;

/**
 * Defines the interface to interact with a {@link Throttle}. The listener 
 * hears/observes throttles which decouples the throttle from the listener.
 * Therefore, the listener knows when the throttle bandwidth availability 
 * changes.
 * 
 */
public interface ThrottleListener {
        
    /** Gets the attachment for the <code>Throttle</code> to recognize. */
    Object getAttachment();
    
    /** Notifies the listener that bandwidth is available and interest should be registered. */
    boolean bandwidthAvailable();
    
    /** Notifies the listener that bandwidth should be requested. */
    void requestBandwidth();
    
    /** Notifies the listener that bandwidth should be released. */
    void releaseBandwidth();
    
    /** Determines if the listener is still open. */
    boolean isOpen();
}