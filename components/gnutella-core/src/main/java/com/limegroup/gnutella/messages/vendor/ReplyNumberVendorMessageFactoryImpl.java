package com.limegroup.gnutella.messages.vendor;

import org.limewire.io.GUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;

@Singleton
public class ReplyNumberVendorMessageFactoryImpl implements ReplyNumberVendorMessageFactory {
    
    private final NetworkManager networkManager;
    
    @Inject
    public ReplyNumberVendorMessageFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public ReplyNumberVendorMessage createFromNetwork(
            byte[] guid, byte ttl, byte hops, int version, byte[] payload, Network network)
            throws BadPacketException {
        return new ReplyNumberVendorMessage(guid, ttl, hops, version, payload, network);
    }

    public ReplyNumberVendorMessage create(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, ReplyNumberVendorMessage.VERSION, numResults, networkManager.canReceiveUnsolicited());
    }

    public ReplyNumberVendorMessage createV2ReplyNumberVendorMessage(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, ReplyNumberVendorMessage.OLD_VERSION, numResults, networkManager.canReceiveUnsolicited());
    }

    public ReplyNumberVendorMessage createV3ReplyNumberVendorMessage(GUID replyGUID, int numResults) {
        return new ReplyNumberVendorMessage(replyGUID, ReplyNumberVendorMessage.VERSION, numResults, networkManager.canReceiveUnsolicited());
    }
    
}
