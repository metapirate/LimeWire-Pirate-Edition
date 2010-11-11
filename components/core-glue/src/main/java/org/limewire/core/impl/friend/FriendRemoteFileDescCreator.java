package org.limewire.core.impl.friend;

import java.util.Set;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;

import com.google.inject.Inject;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescCreator;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Creates {@link FriendRemoteFileDesc} for {@link org.limewire.friend.impl.address.FriendAddress}.
 */
@EagerSingleton
class FriendRemoteFileDescCreator implements RemoteFileDescCreator {

    private final AddressFactory addressFactory;
    private final FriendAddressResolver addressResolver;

    @Inject
    public FriendRemoteFileDescCreator(AddressFactory addressFactory, FriendAddressResolver addressResolver) {
        this.addressFactory = addressFactory;
        this.addressResolver = addressResolver;
    }
    
    @Inject
    void register(RemoteFileDescFactory remoteFileDescFactory) {
        remoteFileDescFactory.register(this);
    }
    
    @Override
    public boolean canCreateFor(Address address) {
        return address instanceof FriendAddress;
    }

    /**
     * Note browseHost and replyToMulticast will be ignored.
     */
    @Override
    public RemoteFileDesc create(Address address, long index, String filename, long size,
            byte[] clientGUID, int speed, int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor,
            long createTime, boolean http1) {
        return new FriendRemoteFileDesc((FriendAddress)address, index, filename, size, clientGUID, speed, quality, xmlDoc, urns, vendor, createTime, true, addressFactory, addressResolver);
    }

}
