package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.collection.IntHashMap;
import org.limewire.inject.EagerSingleton;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * Factory to turn binary input as read from Network to VendorMessage
 * Objects.
 */
@EagerSingleton
public class VendorMessageFactoryImpl implements VendorMessageFactory {
    
    private static final Log LOG = LogFactory.getLog(VendorMessageFactoryImpl.class);
    
    private static final Comparator<byte[]> COMPARATOR = new Comparators.ByteArrayComparator();
    
    /** Map (VendorID -> Map (selector -> Parser)) */
    private volatile Map<byte[], IntHashMap<VendorMessageParser>> VENDORS =
        new TreeMap<byte[], IntHashMap<VendorMessageParser>>(COMPARATOR);
    
    private static final BadPacketException UNRECOGNIZED_EXCEPTION =
        new BadPacketException("Unrecognized Vendor Message");

    public VendorMessageFactoryImpl() {
    }
    
    @Inject
    public VendorMessageFactoryImpl(VendorMessageParserBinder vendorMessageParserBinder) {
        vendorMessageParserBinder.bind(this);
    }
    
    public void setParser(int selector, byte[] vendorId, VendorMessageParser parser) {
        if (selector < 0 || selector > 0xFFFF) {
            throw new IllegalArgumentException("Selector is out of range: " + selector);
        }
        
        if (vendorId == null) {
            throw new NullPointerException("Vendor ID is null");
        }
        
        if (vendorId.length != 4) {
            throw new IllegalArgumentException("Vendor ID must be 4 bytes long");
        }
        
        if (parser == null) {
            throw new NullPointerException("VendorMessageParser is null");
        }
        
        Object o = null;
        synchronized (VENDORS) {
            Map<byte[], IntHashMap<VendorMessageParser>> vendors = copyVendors();

            IntHashMap<VendorMessageParser> selectors = vendors.get(vendorId);
            if (selectors == null) {
                selectors = new IntHashMap<VendorMessageParser>();
                vendors.put(vendorId, selectors);
            }
            
            o = selectors.put(selector, parser);
            VENDORS = vendors;
        }
        
        if (o != null && LOG.isErrorEnabled()) {
            LOG.error("There was already a VendorMessageParser of type " 
                + o.getClass() + " registered for selector " + selector);
        }
    }
    
    /** A helper method to create a deep copy of the VENDORS TreeMap. */
    private Map<byte[], IntHashMap<VendorMessageParser>> copyVendors() {
        Map<byte[], IntHashMap<VendorMessageParser>> copy =
            new TreeMap<byte[], IntHashMap<VendorMessageParser>>(COMPARATOR);
        
        for(Map.Entry<byte[], IntHashMap<VendorMessageParser>> entry : VENDORS.entrySet()) {
            copy.put(entry.getKey(), new IntHashMap<VendorMessageParser>(entry.getValue()));
        }
        
        return copy;
    }
    
    public VendorMessageParser getParser(int selector, byte[] vendorId) {
        IntHashMap<VendorMessageParser> selectors = VENDORS.get(vendorId);
        if (selectors == null) {
            return null;
        }
        
        return selectors.get(selector);
    }
    
    public VendorMessage deriveVendorMessage(byte[] guid, byte ttl,
            byte hops, byte[] fromNetwork, Network network)
            throws BadPacketException {

        // sanity check
        if (fromNetwork.length < VendorMessage.LENGTH_MINUS_PAYLOAD) {
            throw new BadPacketException("Not enough bytes for a VM!!");
        }

        // get very necessary parameters....
        ByteArrayInputStream bais = new ByteArrayInputStream(fromNetwork);
        byte[] vendorID = null, restOf = null;
        int selector = -1, version = -1;
        try {
            // first 4 bytes are vendor ID
            vendorID = new byte[4];
            bais.read(vendorID, 0, vendorID.length);
            // get the selector....
            selector = ByteUtils.ushort2int(ByteUtils.leb2short(bais));
            // get the version....
            version = ByteUtils.ushort2int(ByteUtils.leb2short(bais));
            // get the rest....
            restOf = new byte[bais.available()];
            bais.read(restOf, 0, restOf.length);
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
        }

        VendorMessageParser parser = getParser(selector, vendorID);
        if (parser == null) {
            throw UNRECOGNIZED_EXCEPTION;
        }
        
        return parser.parse(guid, ttl, hops, version, restOf, network);
    }
}
