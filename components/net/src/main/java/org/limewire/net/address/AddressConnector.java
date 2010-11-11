package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.io.AddressConnectingLoggingCategory;
import org.limewire.nio.observer.ConnectObserver;

/**
 * Defines the requirements for an entity that can connect to an address
 * and create a socket.
 * <p>
 * Address connectors can optionally notify sockets manager when their 
 * connectivity has changed by firing an event on EventMulticaster<ConnectivityChangeEvent>
 * which they can have injected.
 */
public interface AddressConnector {
    
    /**
     * Logging category name for AddressConnector's to use.
     */
    public static final String LOGGING_CATEGORY = AddressConnectingLoggingCategory.CATEGORY;

    /**
     * Returns true if it can connect to the given type of address. This must
     * be a non-blocking call. Evaluation should take into account the connectors
     * initialization state.
     */
    boolean canConnect(Address address);

    /**
     * Connects asynchronously to the given address and notifies 
     * <code>observer</code> of the established socket.
     */
    void connect(Address address, ConnectObserver observer);
    
}
