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
import com.limegroup.gnutella.downloader.RemoteFileDescDeserializer;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@EagerSingleton
public class FriendRemoteFileDescDeserializer implements RemoteFileDescDeserializer {

    private final AddressFactory addressFactory;
    private final FriendAddressResolver addressResolver;

    @Inject
    public FriendRemoteFileDescDeserializer(AddressFactory addressFactory, FriendAddressResolver addressResolver) {
        this.addressFactory = addressFactory;
        this.addressResolver = addressResolver;
    }
    
    @Override
    @Inject
    public void register(RemoteFileDescFactory remoteFileDescFactory) {
        remoteFileDescFactory.register(FriendRemoteFileDesc.TYPE, this);
    }
    
    @Override
    public RemoteFileDesc createRemoteFileDesc(Address address, long index, String filename,
            long size, byte[] clientGUID, int speed, int quality, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, String vendor, long createTime) {
        return new FriendRemoteFileDesc((FriendAddress)address, index, filename, size, clientGUID, speed, quality, xmlDoc, urns, vendor, createTime, true, addressFactory, addressResolver);
    }
    
}
