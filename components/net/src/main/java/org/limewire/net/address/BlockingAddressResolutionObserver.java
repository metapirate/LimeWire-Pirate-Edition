package org.limewire.net.address;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.OnewayExchanger;
import org.limewire.io.Address;

/**
 * This class provides a blocking {@link AddressResolutionObserver}.
 */
public class BlockingAddressResolutionObserver implements AddressResolutionObserver {

    private final OnewayExchanger<Address, IOException> exchanger = new OnewayExchanger<Address, IOException>();
    
    @Override
    public void resolved(Address address) {
        exchanger.setValue(address);
    }

    @Override
    public void handleIOException(IOException iox) {
        exchanger.setException(iox);
    }

    @Override
    public void shutdown() {
        exchanger.setException(new IOException("shut down"));
    }

    public Address getAddress() throws IOException {
        try {
            return exchanger.get();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
    
    public Address getAddress(long timeout, TimeUnit timeUnit) throws IOException, TimeoutException {
        try {
            return exchanger.get(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

}
