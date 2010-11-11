package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.listener.DefaultDataTypeEvent;

public class AddressEvent extends DefaultDataTypeEvent<Address, AddressEvent.Type> {

    public static enum Type {
        ADDRESS_CHANGED
    }

    public AddressEvent(Address data, AddressEvent.Type event) {
        super(data, event);
    }
}
