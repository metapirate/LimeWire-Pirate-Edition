package com.limegroup.gnutella;

import java.io.IOException;

import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;

import com.google.inject.Inject;

@EagerSingleton
public class PushEndpointSerializer implements AddressSerializer {

    private final PushEndpointFactory pushEndpointFactory;
    
    private static final String PUSH_ENDPOINT = "PE";

    @Inject
    public PushEndpointSerializer(PushEndpointFactory pushEndpointFactory) {
        this.pushEndpointFactory = pushEndpointFactory;
    }

    @Override
    public Address deserialize(String address) throws IOException {
        throw new IOException();
    }

    @Override
    public Address deserialize(byte[] serializedAddress) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedAddress);
            return pushEndpointFactory.createPushEndpoint(ggep.getString(PUSH_ENDPOINT));
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean canSerialize(Address address) {
        return address instanceof PushEndpoint;
    }

    @Override
    public String getAddressType() {
        return "push-endpoint";
    }

    @Override
    @Inject
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }

    @Override
    public byte[] serialize(Address address) throws IOException {
        GGEP ggep = new GGEP();
        ggep.put(PUSH_ENDPOINT, ((PushEndpoint)address).httpStringValue());
        return ggep.toByteArray();
    }

}
