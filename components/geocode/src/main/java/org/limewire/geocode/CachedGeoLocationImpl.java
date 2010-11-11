package org.limewire.geocode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.geocode.GeocodeInformation.Property;
import org.limewire.inject.EagerSingleton;
import org.limewire.inject.MutableProvider;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Join;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ExternalIP;
import org.limewire.net.address.AddressEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;

@EagerSingleton
public class CachedGeoLocationImpl implements Provider<GeocodeInformation>, Service {

    private static final Log LOG = LogFactory.getLog(CachedGeoLocationImpl.class);

    private final MutableProvider<Properties> settingGeo;
    private final Provider<Geocoder> geocoder;
    /**
     * invariant: never null
     */
    private volatile GeocodeInformation currentGeo = GeocodeInformation.EMPTY_GEO_INFO;
    private final Provider<byte[]> externalAddress;
    /**
     * Keeps track of how often the address has changed.
     */
    private final AtomicInteger ipAddressChangeCount = new AtomicInteger(0);

    @Inject
    public CachedGeoLocationImpl(@GeoLocation MutableProvider<Properties> settingLocation,
                                 Provider<Geocoder> geocoder,
                                 @ExternalIP Provider<byte []> externalAddress) {
        this.settingGeo = settingLocation;
        this.geocoder = geocoder;
        this.externalAddress = externalAddress;
    }
    
    @Inject
    void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }
    
    @Inject
    void register(ListenerSupport<AddressEvent> addressListenerSupport) {
        // the address change event does not necessarily mean the external
        // address change, but it's a good trigger to recheck
        addressListenerSupport.addListener(new EventListener<AddressEvent>() {
            @Override
            @BlockingEvent(queueName = "fetch-geo-location")
            public void handleEvent(AddressEvent event) {
                if (ipAddressChanged()) {
                    if (ipAddressChangeCount.incrementAndGet() < 4) {
                        fetchNewGeoCodeInformation();
                    }
                }
            }
        });
    }

    public GeocodeInformation get() {
        return currentGeo;
    }
    
    private boolean ipAddressChanged() {
        String lastAddress = currentGeo.getProperty(Property.Ip);
        if (lastAddress == null) {
            LOG.debug("no address in saved geolocation");
            return false;
        }
        // check if the info is not stale
        byte[] currentAddress = externalAddress.get();
        if (!NetworkUtils.isValidAddress(currentAddress)) {
            LOG.debug("external address not available yet");
            return false;
        }
        try {
            byte[] lastIp = InetAddress.getByName(lastAddress).getAddress();
            if (LOG.isDebugEnabled()) {
                LOG.debugf("comparing addresses: {0} and {1}", lastAddress, NetworkUtils.ip2string(currentAddress));
            }
            if (!NetworkUtils.isCloseIP(currentAddress, lastIp)) {
                return true;
            } 
        } catch (UnknownHostException e) {
            LOG.warn("Unable to get host by name", e);  
        }
        return false;
    }
    
    @Override
    public void initialize() {
        currentGeo = GeocodeInformation.fromProperties(settingGeo.get());
        LOG.debugf("geo location from settings: {0}", currentGeo);
        // old settings don't have ip set due to bug in server/parsing code,
        // so reset to empty info to trigger refetch in start()
        if (currentGeo.getProperty(Property.Ip) == null) {
            currentGeo = GeocodeInformation.EMPTY_GEO_INFO;
        }
    }

    @Override
    public String getServiceName() {
        return "GeocodeLocation";
    }

    private void fetchNewGeoCodeInformation() {
        LOG.debug("fetching new geo location");
        Geocoder coder = geocoder.get();
        currentGeo = coder.getGeocodeInformation();
        settingGeo.set(currentGeo.toProperties());
        LOG.debugf("new geolocation: {0}", currentGeo);   
    }
    
    @Override
    @Asynchronous(join = Join.NONE)
    public void start() {
        if (currentGeo.isEmpty()) {
            fetchNewGeoCodeInformation();
        }
    }

    @Override
    public void stop() {
    }
}
