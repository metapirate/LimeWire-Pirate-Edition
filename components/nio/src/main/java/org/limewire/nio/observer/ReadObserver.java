package org.limewire.nio.observer;

import java.io.IOException;

/**
 * Defines the interface that allows read events to be received. Classes are
 * notified of new read events through this interface.
 */
public interface ReadObserver extends IOErrorObserver {

    /** Notification that a read can be performed. */
    void handleRead() throws IOException;
}
