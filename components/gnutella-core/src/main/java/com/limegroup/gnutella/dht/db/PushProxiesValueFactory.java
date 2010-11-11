package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

/**
 * Defines an interface to create <code>PushProxiesValue</code>s.
 */
public interface PushProxiesValueFactory extends DHTValueFactory<PushProxiesValue> {

    public PushProxiesValue createDHTValue(DHTValueType type, Version version,
            byte[] value) throws DHTValueException;

    public PushProxiesValue createDHTValueForSelf();
    
}