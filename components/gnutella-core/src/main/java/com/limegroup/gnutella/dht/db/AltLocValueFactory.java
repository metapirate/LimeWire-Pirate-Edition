package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

/**
 * Defines an interface to create alternative location values.
 *
 */
public interface AltLocValueFactory extends DHTValueFactory<AltLocValue> {

    public AltLocValue createDHTValue(DHTValueType type, Version version,
            byte[] value) throws DHTValueException;

    /**
     * Creates an alternative location value.
     */
    public AltLocValue createFromData(Version version, byte[] data)
            throws DHTValueException;

    /**
     * Creates an alternative location value for this node.
     * 
     * @param fileSize size of the file
     * @param ttroot root hash of the TigerTree
     */
    public AltLocValue createAltLocValueForSelf(long fileSize, byte[] ttroot);

}