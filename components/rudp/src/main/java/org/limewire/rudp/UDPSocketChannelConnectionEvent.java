package org.limewire.rudp;

import org.limewire.listener.DefaultDataTypeEvent;

public class UDPSocketChannelConnectionEvent extends DefaultDataTypeEvent<UDPSocketChannel, ConnectionState> {
    public UDPSocketChannelConnectionEvent(UDPSocketChannel data, ConnectionState event) {
        super(data, event);
    }
}
