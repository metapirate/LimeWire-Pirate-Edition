package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.security.MerkleTree;

class AltLocValueImpl extends AbstractAltLocValue {
    
    private static final long serialVersionUID = -6975718782217170657L;

    private final byte[] guid;
    
    private final int port;
    
    private final long fileSize;
    
    private final byte[] ttroot;
    
    private final boolean firewalled;
    
    private final byte[] data;
    
    private final boolean supportsTLS;
    
    /**
     * Constructor for testing purposes
     */
    protected AltLocValueImpl(Version version, byte[] guid, int port, 
            long fileSize, byte[] ttroot, boolean firewalled, boolean supportsTLS) {
        super(version);
        
        if (guid == null || guid.length != 16) {
            throw new IllegalArgumentException("Illegal GUID");
        }
        
        if (!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("Illegal port: " + port);
        }
        
        if (version.compareTo(AbstractAltLocValue.VERSION_ONE) >= 0) {
            if (fileSize < 0L) {
                throw new IllegalArgumentException("Illegal fileSize: " + fileSize);
            }
            
            if (ttroot != null && ttroot.length != MerkleTree.HASHSIZE) {
                throw new IllegalArgumentException("Illegal ttroot length: " + ttroot.length);
            }
        }
        
        this.guid = guid;
        this.port = port;
        this.fileSize = fileSize;
        this.ttroot = ttroot;
        this.firewalled = firewalled;
        this.supportsTLS = supportsTLS;
        this.data = AbstractAltLocValue.serialize(this);
    }
    
    public AltLocValueImpl(Version version, byte[] data) throws DHTValueException {
        super(version);
        
        if (version == null) {
            throw new DHTValueException("Version is null");
        }
        
        if (data == null) {
            throw new DHTValueException("Data is null");
        }
        
        this.data = data;
        
        try {
            GGEP ggep = new GGEP(data, 0);
            
            this.guid = ggep.getBytes(AbstractAltLocValue.CLIENT_ID);
            if (guid.length != 16) {
                throw new DHTValueException("Illegal GUID length: " + guid.length);
            }
            
            byte[] portBytes = ggep.getBytes(AbstractAltLocValue.PORT);
            this.port = ByteUtils.beb2short(portBytes, 0) & 0xFFFF;
            if (!NetworkUtils.isValidPort(port)) {
                throw new DHTValueException("Illegal port: " + port);
            }
            
            byte[] firewalled = ggep.getBytes(AbstractAltLocValue.FIREWALLED);
            if (firewalled.length != 1) {
                throw new DHTValueException("Illegal Firewalled length: " + firewalled.length);
            }
            
            this.firewalled = (firewalled[0] != 0);
            
            this.supportsTLS = ggep.hasKey(AbstractAltLocValue.TLS);
            
            if (version.compareTo(AbstractAltLocValue.VERSION_ONE) >= 0) {
                
                this.fileSize = ggep.getLong(AbstractAltLocValue.LENGTH);
                
                if (ggep.hasKey(AbstractAltLocValue.TTROOT)) {
                    byte[] ttroot = ggep.getBytes(AbstractAltLocValue.TTROOT);
                    if (ttroot.length != MerkleTree.HASHSIZE) {
                        throw new DHTValueException("Illegal ttroot length: " + ttroot.length);
                    }
                    this.ttroot = ttroot;
                    
                } else {
                    this.ttroot = null;
                }
            } else {
                this.fileSize = -1L;
                this.ttroot = null;
            }
            
        } catch (BadGGEPPropertyException err) {
            throw new DHTValueException(err);
            
        } catch (BadGGEPBlockException err) {
            throw new DHTValueException(err);
        }
    }
    
    @Override
    public int getPort() {
        return port;
    }
    
    @Override
    public byte[] getGUID() {
        return guid;
    }
    
    @Override
    public boolean isFirewalled() {
        return firewalled;
    }
    
    @Override
    public boolean supportsTLS() {
        return supportsTLS;
    }
    
    @Override
    public long getFileSize() {
        return fileSize;
    }
    
    @Override
    public byte[] getRootHash() {
        return ttroot;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#getValue()
     */
    public byte[] getValue() {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValue#write(java.io.OutputStream)
     */
    public void write(OutputStream out) throws IOException {
        out.write(data);
    }
}