package com.limegroup.gnutella.dht.db;

import java.util.Arrays;

import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.ArrayUtils;
import org.limewire.util.ByteUtils;


/**
 * An implementation of DHTValue for for Gnutella Alternate Locations.
 */
public abstract class AbstractAltLocValue implements AltLocValue {
    
    /**
     * DHTValueType for AltLocs.
     */
    public static final DHTValueType ALT_LOC = DHTValueType.valueOf("Gnutella Alternate Location", "ALOC");
    
    /*
     * AltLocValue version history
     * 
     * Version 0:
     * GUID
     * Port
     * Firewalled
     * 
     * Version 1:
     * File Length
     * TigerTree root hash (optional)
     * Incoming TLS support (optional)
     */
    
    public static final Version VERSION_ONE = Version.valueOf(1);
    
    /**
     * Version of AltLocDHTValue.
     */
    public static final Version VERSION = VERSION_ONE;
    
    static final String CLIENT_ID = "client-id";
    
    static final String PORT = "port";
    
    static final String FIREWALLED = "firewalled";
    
    static final String LENGTH = "length";
    
    static final String TTROOT = "ttroot";
    
    static final String TLS = "tls";
    
    protected final Version version;
    
    public AbstractAltLocValue(Version version) {
        this.version = version;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getVersion()
     */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#getVersion()
     */
    public Version getVersion() {
        return version;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValueType()
     */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#getValueType()
     */
    public DHTValueType getValueType() {
        return ALT_LOC;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#size()
     */
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#size()
     */
    public int size() {
        return getValue().length;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#getGUID()
     */
    public abstract byte[] getGUID();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#getPort()
     */
    public abstract int getPort();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#getFileSize()
     */
    public abstract long getFileSize();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#getRootHash()
     */
    public abstract byte[] getRootHash();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#isFirewalled()
     */
    public abstract boolean isFirewalled();
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.AltLocValue#supportsTLS()
     */
    public abstract boolean supportsTLS();

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AltLocValue)) {
            return false;
        }
        AltLocValue other = (AltLocValue)obj;
        return getFileSize() == other.getFileSize() && Arrays.equals(getGUID(), other.getGUID())
                && getPort() == other.getPort() && Arrays.equals(getRootHash(), other.getRootHash())
                && Arrays.equals(getValue(), other.getValue()) && getValueType().equals(other.getValueType())
                && getVersion().equals(other.getVersion());
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int)getFileSize();
        hash = 31 * hash + getPort();
        hash = 31 * hash + getGUID().hashCode();
        hash = 31 * hash + getRootHash().hashCode();
        hash = 31 * hash + getValue().hashCode();
        hash = 31 * hash + getValueType().hashCode();
        hash = 31 * hash + getVersion().hashCode();
        return hash;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("AltLoc: guid=").append(new GUID(getGUID()))
            .append(", port=").append(getPort())
            .append(", firewalled=").append(isFirewalled())
            .append(", tls=").append(supportsTLS())
            .append(", fileSize=").append(getFileSize())
            .append(", ttroot=").append(getRootHash() != null 
                    ? ArrayUtils.toHexString(getRootHash()) : "null");
        
        if (this instanceof AltLocValueForSelf) {
            buffer.append(", local=true");
        }
        
        return buffer.toString();
    }
    
    /**
     * A helper method to serialize AltLocValues.
     */
    protected static byte[] serialize(AbstractAltLocValue value) {
        Version version = value.getVersion();
        
        GGEP ggep = new GGEP();
        
        ggep.put(CLIENT_ID, value.getGUID());
        
        byte[] port = new byte[2];
        ByteUtils.short2beb((short)value.getPort(), port, 0);
        ggep.put(PORT, port);
        
        byte[] firewalled = { (byte)(value.isFirewalled() ? 1 : 0) };
        ggep.put(FIREWALLED, firewalled);
        
        if (version.compareTo(VERSION_ONE) >= 0) {
            ggep.put(LENGTH, /* long */ value.getFileSize());
            
            byte[] ttroot = value.getRootHash();
            if (ttroot != null) {
                ggep.put(TTROOT, ttroot);
            }
            
            if (value.supportsTLS())
                ggep.put(TLS);
        }
        
        return ggep.toByteArray();
    }
}
