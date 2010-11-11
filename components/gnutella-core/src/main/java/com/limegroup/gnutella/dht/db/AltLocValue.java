package com.limegroup.gnutella.dht.db;

import java.io.Serializable;

import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

/**
 * Defines an interface of an alternative location DHT value.
 */
public interface AltLocValue extends DHTValue, Serializable {

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getVersion()
     */
    public Version getVersion();

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValueType()
     */
    public DHTValueType getValueType();

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#size()
     */
    public int size();

    /**
     * The GUID of the AltLoc.
     */
    public byte[] getGUID();

    /**
     * The (Gnutella) Port of the AltLoc.
     */
    public int getPort();

    /**
     * The length of the file.
     */
    public long getFileSize();

    /**
     * The TigerTree root hash.
     */
    public byte[] getRootHash();

    /**
     * Returns true if the AltLoc is firewalled.
     */
    public boolean isFirewalled();

    /**
     * @return true if the alternative location supports TLS
     */
    public boolean supportsTLS();

}