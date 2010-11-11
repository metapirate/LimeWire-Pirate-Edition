package com.limegroup.gnutella.downloader.serial;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.util.UnboxUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescImpl;

/** A memento for a remote host. */
public class RemoteHostMemento implements Serializable {

    private static final Log LOG = LogFactory.getLog(RemoteHostMemento.class);
    
    private static final long serialVersionUID = 1452696797555431199L;

    // NOTE: Never remove items, only add.  Otherwise, mementos serialized with earlier
    // keys cannot be deserialized.
    private static enum Keys {
        HOST, PORT, FILENAME, INDEX, CLIENTGUID,
        SPEED, SIZE, CHAT, QUALITY, REPLY_TO_MULTICAST,
        XML, URNS, BH, FIREWALLED, VENDOR, HTTP11,
        TLS, PUSH_ADDR, CUSTOM_URL, ADDRESS, TYPE
    }
    
    private final Map<Keys, Serializable> propertiesMap;
    
    public RemoteHostMemento(Address address, String filename, long index, byte[] clientGuid,
            int speed, long size, int quality, boolean replyToMulticast, String xml, Set<URN> urns,
            boolean browseHost, String vendor, boolean http1, String type, AddressFactory addressFactory) {

        this.propertiesMap = new HashMap<Keys, Serializable>(Keys.values().length);

        propertiesMap.put(Keys.FILENAME, filename);
        propertiesMap.put(Keys.INDEX, index);
        propertiesMap.put(Keys.CLIENTGUID, clientGuid);
        propertiesMap.put(Keys.SPEED, speed);
        propertiesMap.put(Keys.SIZE, size);
        propertiesMap.put(Keys.QUALITY, quality);
        propertiesMap.put(Keys.REPLY_TO_MULTICAST, replyToMulticast);
        propertiesMap.put(Keys.XML, xml);
        propertiesMap.put(Keys.URNS, (Serializable) urns);
        propertiesMap.put(Keys.BH, browseHost);
        propertiesMap.put(Keys.VENDOR, vendor);
        propertiesMap.put(Keys.HTTP11, http1);
        propertiesMap.put(Keys.ADDRESS, serializeAddress(address, addressFactory));
        propertiesMap.put(Keys.TYPE, type);
    }

    /**
     * Encodes address as address-type:utf8(base64(serialized-address)).
     */
    public static String serializeAddress(Address address, AddressFactory addressFactory) {
        AddressSerializer serializer = addressFactory.getSerializer(address);
        assert serializer != null : "for address class: " + address.getClass();
        StringBuilder builder = new StringBuilder(serializer.getAddressType());
        builder.append(":");
        try {
            builder.append(StringUtils.getUTF8String(Base64.encodeBase64(serializer.serialize(address))));
            return builder.toString();
        } catch (IOException e) {
            // impossible
            throw new RuntimeException(e);
        }
    }
    
    static Address deserializeAddress(final String addressString, AddressFactory addressFactory) {
        StringTokenizer st = new StringTokenizer(addressString, ":");
        if (st.hasMoreTokens()) {
            String type = st.nextToken();
            if (st.hasMoreTokens()) {
                String addressPart = st.nextToken();
                AddressSerializer serializer = addressFactory.getSerializer(type);
                if (serializer != null) {
                    try {
                        return serializer.deserialize(Base64.decodeBase64(StringUtils.toUTF8Bytes(addressPart)));
                    } catch (IOException e) {
                        LOG.debug("", e);
                    }
                }
                LOG.debug("not enough arguments");
            }
        }
        
        return new Address() {
            @Override
            public String getAddressDescription() {
                return addressString;
            }
        };
    }

    public String getHost() { return (String)propertiesMap.get(Keys.HOST); }    
    public int getPort() { return UnboxUtils.toInt((Integer)propertiesMap.get(Keys.PORT)); }
    public String getFileName() { return (String)propertiesMap.get(Keys.FILENAME); }
    public long getIndex() { return UnboxUtils.toLong((Long)propertiesMap.get(Keys.INDEX)); }
    public byte[] getClientGuid() { return (byte[])propertiesMap.get(Keys.CLIENTGUID); }
    public int getSpeed() { return UnboxUtils.toInt((Integer)propertiesMap.get(Keys.SPEED)); }
    public long getSize() { return UnboxUtils.toLong((Long)propertiesMap.get(Keys.SIZE)); }
    public int getQuality() { return UnboxUtils.toInt((Integer)propertiesMap.get(Keys.QUALITY)); }
    public boolean isReplyToMulticast() { return UnboxUtils.toBoolean((Boolean)propertiesMap.get(Keys.REPLY_TO_MULTICAST)); }
    public String getXml() { return (String)propertiesMap.get(Keys.XML); }
    @SuppressWarnings("unchecked")
    public Set<URN> getUrns() { return (Set<URN>)propertiesMap.get(Keys.URNS); }
    public boolean isBrowseHost() { return UnboxUtils.toBoolean((Boolean)propertiesMap.get(Keys.BH)); }
    public String getVendor() { return (String)propertiesMap.get(Keys.VENDOR); }
    public boolean isHttp11() { return UnboxUtils.toBoolean((Boolean)propertiesMap.get(Keys.HTTP11)); }
    public boolean isTls() { return UnboxUtils.toBoolean((Boolean)propertiesMap.get(Keys.TLS)); }
    public String getPushAddr() { return (String)propertiesMap.get(Keys.PUSH_ADDR); }
    public URL getCustomUrl() { return (URL)propertiesMap.get(Keys.CUSTOM_URL); }
    
    /**
     * Returns the type of the remote host memento which is used for deserializing
     * the correct type of {@link RemoteFileDesc}.
     * 
     * @return {@link RemoteFileDescImpl#TYPE} in case no type is set
     */
    public String getType() { 
        String type = (String) propertiesMap.get(Keys.TYPE);
        if (type != null) {
            return type;
        }
        return RemoteFileDescImpl.TYPE;
    }
    
    public Address getAddress(AddressFactory addressFactory, PushEndpointFactory pushEndpointFactory) throws IOException {
        return getAddress((String) propertiesMap.get(Keys.ADDRESS), addressFactory, pushEndpointFactory);
    }
        
    public Address getAddress(String address, AddressFactory addressFactory, PushEndpointFactory pushEndpointFactory) throws IOException {
        if (address != null) {
            return deserializeAddress(address, addressFactory);
        }
        String pushAddress = getPushAddr();
        if (pushAddress != null) {
            return pushEndpointFactory.createPushEndpoint(pushAddress); 
        }
        return new ConnectableImpl(getHost(), getPort(), isTls());
    }
    
    public void setCustomUrl(URL url) {
        propertiesMap.put(Keys.CUSTOM_URL, url);
    }
    
}
