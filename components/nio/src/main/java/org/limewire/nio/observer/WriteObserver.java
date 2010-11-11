package org.limewire.nio.observer;

/**
 * Defines the interface to allows write events to be received.
 * <p>
 * If the events are being received because of a <code>SelectableChannel</code>,
 * you can turn off interest in events via 
 * <code>NIODispatcher.instance().interestWrite(channel, false);</code>
 */
public interface WriteObserver extends IOErrorObserver {

    /**
     * Notification that a write can be performed.
     * @return <code>true</code> there is still data to be written, otherwise 
     * <code>false</code>.
     */
    boolean handleWrite() throws java.io.IOException;
    
}