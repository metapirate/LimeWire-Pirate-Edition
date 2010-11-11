package org.limewire.nio.channel;

import org.limewire.nio.observer.ReadObserver;


/**
 * Defines an interface that combines the <code>ReadObserver</code> and 
 * <code>ChannelReader</code> interface.
 */
public interface ChannelReadObserver extends ReadObserver, ChannelReader {
}
