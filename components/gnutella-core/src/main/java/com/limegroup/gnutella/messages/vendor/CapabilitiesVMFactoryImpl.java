package com.limegroup.gnutella.messages.vendor;

import java.util.Map;
import java.util.TreeMap;

import org.limewire.collection.Comparators;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.FeatureSearchData;

@Singleton
public class CapabilitiesVMFactoryImpl implements CapabilitiesVMFactory {

    private final Provider<DHTManager> dhtManager;
    private final Provider<NetworkManager> networkManager;
    private volatile CapabilitiesVM currentCapabilities;

    @Inject
    public CapabilitiesVMFactoryImpl(Provider<DHTManager> dhtManager,
            Provider<NetworkManager> networkManager) {
        this.dhtManager = dhtManager;
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory#getCapabilitiesVM()
     */
    public CapabilitiesVM getCapabilitiesVM() {
        if (currentCapabilities == null)
            currentCapabilities = new CapabilitiesVMImpl(getSupportedMessages());
        return currentCapabilities;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory#updateCapabilities()
     */
    public void updateCapabilities() {
        currentCapabilities = new CapabilitiesVMImpl(getSupportedMessages());
    }

    /**
     * Adds all supported capabilities to the given set.
     */
    // protected for testing
    protected Map<byte[], Integer> getSupportedMessages() {
        Map<byte[], Integer> supported = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
        
        // old shutoff capabilities -- the maxid for each.
        supported.put(new byte[] {'I', 'M', 'P', 'P' }, 2147483647);
        supported.put(new byte[] { 'L', 'M', 'U', 'P' }, 2147483647);
        
        supported.put(CapabilitiesVM.FEATURE_SEARCH_BYTES, FeatureSearchData.FEATURE_SEARCH_MAX_SELECTOR);
        supported.put(CapabilitiesVM.INCOMING_TCP_BYTES, networkManager.get().acceptedIncomingConnection() ? 1 : 0);
        supported.put(CapabilitiesVM.FWT_SUPPORT_BYTES, networkManager.get().supportsFWTVersion());
        
        if (dhtManager.get().isMemberOfDHT()) {
            DHTMode mode = dhtManager.get().getDHTMode();
            assert (mode != null);
            supported.put(mode.getCapabilityName(), dhtManager.get().getVersion().shortValue());
        }

        if (networkManager.get().isIncomingTLSEnabled()) {
            supported.put(CapabilitiesVM.TLS_SUPPORT_BYTES, 1);
        }

        return supported;
    }

}
