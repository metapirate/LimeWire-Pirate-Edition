package com.limegroup.gnutella.dht.db;

import java.util.Set;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.io.IpPort;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;

/**
 * Factory to create {@link PushProxiesValue}s.
 */
@Singleton
public class PushProxiesValueFactoryImpl implements PushProxiesValueFactory {

    private final NetworkManager networkManager;
    private final PushEndpointFactory pushEndpointFactory;
    private final Provider<PushProxiesValue> lazySelf;
    private final ApplicationServices applicationServices;

    @Inject
    public PushProxiesValueFactoryImpl(NetworkManager networkManager,
            PushEndpointFactory pushEndpointFactory, ApplicationServices applicationServices) {
        this.networkManager = networkManager;
        this.pushEndpointFactory = pushEndpointFactory;
        this.applicationServices = applicationServices;

        lazySelf = new AbstractLazySingletonProvider<PushProxiesValue>() {
            @Override
            protected PushProxiesValue createObject() {
                return new PushProxiesValueForSelf(
                        PushProxiesValueFactoryImpl.this.networkManager,
                        PushProxiesValueFactoryImpl.this.pushEndpointFactory,
                        PushProxiesValueFactoryImpl.this.applicationServices);
            }
        };
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValueFactory#createDHTValue(org.limewire.mojito.db.DHTValueType, org.limewire.mojito.routing.Version, byte[])
     */
    public PushProxiesValue createDHTValue(DHTValueType type, Version version,
            byte[] value) throws DHTValueException {

        return createFromData(version, value);
    }


    public PushProxiesValue createDHTValueForSelf() {
        return lazySelf.get();
    }
    
    /**
     * Factory method for testing purposes
     */
    AbstractPushProxiesValue createPushProxiesValue(Version version,
            byte[] guid, byte features, int fwtVersion, int port,
            Set<? extends IpPort> proxies) {
        return new PushProxiesValueImpl(version, guid, features, fwtVersion,
                port, proxies);
    }

    /**
     * Factory method to create PushProxiesValues
     */
    PushProxiesValue createFromData(Version version, byte[] data)
            throws DHTValueException {
        return new PushProxiesValueImpl(version, data);
    }
}
