package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.io.AddressConnectingLoggingCategory;

/**
 * Defines the requirements for an entity that can resolve addresses to other addresses.
 */
public interface AddressResolver {

    /**
     * Logging category name for AddressResolver's to use.
     */
    public static final String LOGGING_CATEGORY = AddressConnectingLoggingCategory.CATEGORY;

    /**
     * Returns true if it can resolve the given address. This means it should
     * take its own state and the information provided by the address into
     * account. This call must be non-blocking.
     */
    boolean canResolve(Address address);
    
    /**
     * Asynchronously resolves the address to possibly several other addresses and
     * notifies <code>observer</code> of the resolved addresses.
     * 
     * @return the observer to allow for fluent access
     */
    <T extends AddressResolutionObserver> T resolve(Address address, T observer);
    
}
