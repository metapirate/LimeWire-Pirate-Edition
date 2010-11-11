package org.limewire.nio.observer;

import java.io.IOException;

/**
 * Defines an interface to handle <code>IOExceptions</code> generated during NIO 
 * dispatching.
 */
public interface IOErrorObserver extends Shutdownable {
    
    /** Notification that an <code>IOException</code> occurred while dispatching. */
    void handleIOException(IOException iox);
}