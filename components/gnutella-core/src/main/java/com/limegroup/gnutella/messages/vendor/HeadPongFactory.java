package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;

public interface HeadPongFactory {

    public HeadPong createFromNetwork(byte[] guid, byte ttl, byte hops,
            int version, byte[] payload, Network network) throws BadPacketException;

    public HeadPong create(HeadPongRequestor ping);

}