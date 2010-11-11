package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.nio.observer.IOErrorObserver;

/**
 * Defines the callback that can be notified of a finished address resolution.
 */
public interface AddressResolutionObserver extends IOErrorObserver {

    /**
     * Called with the resolved address. 
     */
    void resolved(Address address);
}
