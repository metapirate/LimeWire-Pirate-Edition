package org.limewire.friend.impl.address;

import java.io.IOException;

import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.util.EmailAddressUtils;

import com.google.inject.Inject;

/**
 * Serializes and deserializes {@link FriendAddress} objects.
 */
@EagerSingleton
public class FriendAddressSerializer implements AddressSerializer {

    static final String JID = "JID";
    
    @Override
    @Inject
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }
    
    @Override
    public boolean canSerialize(Address address) {
        return address instanceof FriendAddress;
    }

    @Override
    public String getAddressType() {
        return "xmpp-address";
    }

    public Address deserialize(final String address) throws IOException {
        if (EmailAddressUtils.isValidAddress(address))
            return new FriendAddress(address);
        throw new IOException();
    }
    
    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedAddress);
            return new FriendAddress(ggep.getString(JID));
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        FriendAddress friendAddress = (FriendAddress)address;
        GGEP ggep = new GGEP();
        ggep.put(JID, friendAddress.getFullId());
        return ggep.toByteArray();
    }

}
