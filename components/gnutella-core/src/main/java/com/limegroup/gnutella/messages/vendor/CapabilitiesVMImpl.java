package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import org.limewire.collection.Comparators;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.Message;

public class CapabilitiesVMImpl extends AbstractVendorMessage implements CapabilitiesVM {
    
    /** The capabilities supported. */
    private final Map<byte[], Integer> capabilities;

    /**
     * Constructs a new CapabilitiesVM from data read off the network.
     */
    CapabilitiesVMImpl(byte[] guid, byte ttl, byte hops, 
                   int version, byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_CAPABILITIES, version,
              payload, network);
        capabilities = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
        
        // populate the Set of supported messages....
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            int vectorSize = ByteUtils.ushort2int(ByteUtils.leb2short(bais));
            // constructing the SMB will cause a BadPacketException if the
            // network data is invalid
            for (int i = 0; i < vectorSize; i++) {
                readCapability(bais, false);
            }
            
            if(bais.available() > 0) {
                vectorSize = ByteUtils.ushort2int(ByteUtils.leb2short(bais));
                for(int i = 0; i < vectorSize; i++) {
                    readCapability(bais, true);
                }
            }
        } catch (IOException ioe) {
            throw new BadPacketException(ioe);
        }
    }


    /**
     * Internal constructor for creating the sole instance of our 
     * CapabilitiesVM.
     */
    CapabilitiesVMImpl(Map<byte[], Integer> _capabilitiesSupported) {
        super(F_NULL_VENDOR_ID, F_CAPABILITIES, VERSION, derivePayload(_capabilitiesSupported));
        this.capabilities = _capabilitiesSupported;
    }
    
    /**
     * Generates the default payload, using all our supported messages.
     */
    private static byte[] derivePayload(Map<byte[], Integer> allCapabilities) {
        try {
            Map<byte[], Integer> capsNeedingInt = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteUtils.short2leb((short)allCapabilities.size(), out);
            for(Map.Entry<byte[], Integer> entry : allCapabilities.entrySet()) {
                writeCapability(out, entry.getKey(), entry.getValue(), false);
                if(entry.getValue() > 65535)
                    capsNeedingInt.put(entry.getKey(), entry.getValue());
            }
            if(capsNeedingInt.size() > 0) {
                ByteUtils.short2leb((short)capsNeedingInt.size(), out);
                for(Map.Entry<byte[], Integer> entry : capsNeedingInt.entrySet()) {
                    writeCapability(out, entry.getKey(), entry.getValue(), true);
                }   
            }
            return out.toByteArray();
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
            return null;
        }

    }
    
    /**
     * @return -1 if the ability isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsCapability(byte[] capabilityName) {
        Integer version = capabilities.get(capabilityName);
        if(version == null || version <= -1)
            return -1;
        else
            return version;
    }
    
    /**
     * Return 1 or higher if TLS is supported by the connection.
     * This does not necessarily mean the connection is over
     * TLS though.
     */
    public int supportsTLS() {
        return supportsCapability(TLS_SUPPORT_BYTES);
    }


    /** @return 1 or higher if capability queries are supported.  The version
     *  number gives some indication about what exactly is a supported.  If no
     *  support, returns -1.
     */
    public int supportsFeatureQueries() {
        return supportsCapability(FEATURE_SEARCH_BYTES);
    }
    

    /** @return true if 'what is new' capability query feature is supported
     */
    public boolean supportsWhatIsNew() {
        return FeatureSearchData.supportsWhatIsNew(
            supportsCapability(FEATURE_SEARCH_BYTES));
    }
    
    /**
     * Returns the current DHT version if this node is an ACTIVE DHT node.
     */
    public int isActiveDHTNode() {
        return supportsCapability(DHTMode.ACTIVE.getCapabilityName());
    }
    
    /**
     * Returns the current DHT version if this node is an PASSIVE DHT node.
     */
    public int isPassiveDHTNode() {
        return supportsCapability(DHTMode.PASSIVE.getCapabilityName());
    }

    /**
     * Returns the current DHT version if this node is an PASSIVE_LEAF DHT node.
     */
    public int isPassiveLeafNode() {
        return supportsCapability(DHTMode.PASSIVE_LEAF.getCapabilityName());
    }
    
    /**
     * @return true unless the remote host indicated they can't accept 
     * incoming tcp. If they didn't say anything we assume they can
     */
    public boolean canAcceptIncomingTCP() {
        return supportsCapability(INCOMING_TCP_BYTES) != 0;
    }
    
    /**
     * @return true unless the remote host indicated they can't do 
     * firewall-to-firewall transfers. If they didn't say anything we assume they can
     */
    public boolean canDoFWT() {
        return supportsCapability(FWT_SUPPORT_BYTES) != 0;
    }
    
    // override super
    @Override
    public boolean equals(Object other) {
        if(other == this)
            return true;
        
        // two of these messages are the same if the support the same messages
        if (other instanceof CapabilitiesVMImpl) {
            CapabilitiesVMImpl vmp = (CapabilitiesVMImpl) other;
            return capabilities.equals(vmp.capabilities);
        }

        return false;
    }
    
    // override super
    @Override
    public int hashCode() {
        return capabilities.hashCode();
    }
    
    private void readCapability(InputStream input, boolean allow4ByteVersion) throws IOException {
        int required = allow4ByteVersion ? 8 : 6;
        
        if (input.available() < required)
            throw new IOException("invalid block.");
        
        byte[] name = new byte[4];
        input.read(name, 0, name.length);
        
        int version = allow4ByteVersion ?
                ByteUtils.leb2int(input) : 
                ByteUtils.ushort2int(ByteUtils.leb2short(input));
                
        capabilities.put(name, version);
    }
    
    static void writeCapability(OutputStream out, byte[] name, int version, boolean allow4ByteVersion) throws IOException {
        out.write(name);
        if(allow4ByteVersion)
            ByteUtils.int2leb(version, out);
        else
            ByteUtils.short2leb((short)version, out);
    }

    /** Overridden purely for stats handling.
     */
    @Override
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
    }

    @Override
    public String toString() {
        return "{CapabilitiesVM:"+super.toString()+"; supporting: " + capabilities + "}";
    }
    
    @Override
    public Class<? extends Message> getHandlerClass() {
        return CapabilitiesVM.class;
    }



}
