package org.limewire.net.address;

import java.io.IOException;
import java.util.Set;

import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.Connectable;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;

import com.google.inject.Inject;

@EagerSingleton
public class FirewalledAddressSerializer implements AddressSerializer {

    static final String PUBLIC_ADDRESS = "PU";
    static final String PRIVATEADDRESS = "PR";
    static final String PROXIES = "PX";
    static final String FWT_VERSION = "FW";
    static final String GUID = "GU";
    
    private final ConnectableSerializer serializer;

    @Inject
    public FirewalledAddressSerializer(ConnectableSerializer serializer) {
        this.serializer = serializer;
    }
    
    @Inject
    @Override
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }

    @Override
    public boolean canSerialize(Address address) {
        return address instanceof FirewalledAddress;
    }

    @Override
    public String getAddressType() {
        return "firewalled-address";
    }
    
    @Override
    public FirewalledAddress deserialize(byte[] serializedAddress) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedAddress, 0);
            Connectable publicAddress = serializer.deserialize(ggep.getBytes(PUBLIC_ADDRESS));
            Connectable privateAddress = serializer.deserialize(ggep.getBytes(PRIVATEADDRESS));
            GUID clientGuid = new GUID(ggep.getBytes(GUID));
            Set<Connectable> pushProxies = serializer.deserializeSet(ggep.getBytes(PROXIES));
            int fwtVersion = ggep.getInt(FWT_VERSION);
            return new FirewalledAddress(publicAddress, privateAddress, clientGuid, pushProxies, fwtVersion);
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] serialize(Address addr) throws IOException {
        FirewalledAddress address = (FirewalledAddress)addr;
        GGEP ggep = new GGEP();
        ggep.put(PUBLIC_ADDRESS, serializer.serialize(address.getPublicAddress()));
        ggep.put(PRIVATEADDRESS, serializer.serialize(address.getPrivateAddress()));
        ggep.put(PROXIES, serializer.serialize(address.getPushProxies()));
        ggep.put(FWT_VERSION, address.getFwtVersion());
        ggep.put(GUID, address.getClientGuid().bytes());
        return ggep.toByteArray();
    }

    @Override
    public Address deserialize(String address) throws IOException {
        throw new IOException();
    }
}
