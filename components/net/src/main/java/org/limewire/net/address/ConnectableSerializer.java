package org.limewire.net.address;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.nio.ByteOrder;
import java.util.Set;

import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;

import com.google.inject.Inject;

@EagerSingleton
public class ConnectableSerializer implements AddressSerializer {
    
    private static final int IP_V4 = 0;
    private static final int IP_V6 = 1;
    
    static final String CONNECTABLE = "CN";
    
    public String getAddressType() {
        return "direct-connect";
    }

    @Override
    public boolean canSerialize(Address address) {
        return address instanceof Connectable;
    }

    public Address deserialize(String address) throws IOException {
        if(address.indexOf(':') == -1) {
            address += ":6346";
        }
        return NetworkUtils.parseIpPort(address, false);
    }
    
    public Connectable deserialize(byte[] serializedAddress) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedAddress);
            InputStream in = new ByteArrayInputStream(ggep.getBytes(CONNECTABLE));
            int hostPortLength = (IOUtils.readByte(in) == IP_V4 ? 4 : 16) + 2;
            byte[] hostPort = new byte[hostPortLength];
            IOUtils.readFully(in, hostPort);
            try {
                IpPort ipPort = NetworkUtils.getIpPort(hostPort, ByteOrder.BIG_ENDIAN);
                boolean supportsTLS = IOUtils.readByte(in) == (byte)1;
                return new ConnectableImpl(ipPort, supportsTLS);
            } catch (InvalidDataException e) {
                throw new IOException(e);
            }
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }
    
    public Set<Connectable> deserializeSet(byte[] serializedSet) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedSet);
            StrictIpPortSet<Connectable> set = new StrictIpPortSet<Connectable>();    
            for (int i = 0; ggep.hasValueFor(CONNECTABLE + i); i++) {
                set.add(deserialize(ggep.getBytes(CONNECTABLE + i)));
            }
            return set;
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }

    public byte[] serialize(Address address) throws IOException {
        Connectable connectable = (Connectable)address;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int type = connectable.getInetAddress() instanceof Inet4Address ? IP_V4 : IP_V6;
        bos.write(type);
        bos.write(NetworkUtils.getBytes(connectable, ByteOrder.BIG_ENDIAN));
        bos.write(connectable.isTLSCapable() ? (byte)1 : (byte) 0);
        GGEP ggep = new GGEP();
        ggep.put(CONNECTABLE, bos.toByteArray());
        return ggep.toByteArray();
    }
    
    public byte[] serialize(Set<Connectable> addresses) throws IOException {
        GGEP ggep = new GGEP();
        int i = 0;
        for (Connectable connectable : addresses) {
            ggep.put(CONNECTABLE + i, serialize(connectable));
            ++i;
        }
        return ggep.toByteArray();
    }

    @Inject
    public void register(AddressFactory factory) {
        factory.registerSerializer(this);
    }
}
