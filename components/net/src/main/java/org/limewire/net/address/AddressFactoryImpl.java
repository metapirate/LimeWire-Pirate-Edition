package org.limewire.net.address;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Singleton;

@Singleton
public class AddressFactoryImpl implements AddressFactory {
    
    private static final Log LOG = LogFactory.getLog(AddressFactoryImpl.class);

    private final ConcurrentHashMap<String, AddressSerializer> serializerTypeMap = new ConcurrentHashMap<String, AddressSerializer>();
    
    public void registerSerializer(AddressSerializer serializer) {
        LOG.debugf("adding serializer: {0}", serializer);
        serializerTypeMap.put(serializer.getAddressType(), serializer);
    }

    @Override
    public AddressSerializer getSerializer(Address address) {
        for (AddressSerializer serializer : serializerTypeMap.values()) {
            if (serializer.canSerialize(address)) {
                return serializer;
            }
        }
        throw new IllegalArgumentException("no serializer available for: " + address);
    }

    @Override
    public AddressSerializer getSerializer(String addressType) {
        return serializerTypeMap.get(addressType);
    }

    @Override
    public Address deserialize(String type, byte[] serializedAddress) throws IOException {
        AddressSerializer serializer = serializerTypeMap.get(type);
        if(serializer != null) {
            return serializer.deserialize(serializedAddress);
        }
        throw new IOException("unknown message type: " + type);
    }

    @Override
    public Address deserialize(String address) throws IOException {
        for(AddressSerializer serializer : serializerTypeMap.values()) {
            try {
                return serializer.deserialize(address);
            } catch (IOException ioe) {
            }
        }
        throw new IOException();
    }
}
