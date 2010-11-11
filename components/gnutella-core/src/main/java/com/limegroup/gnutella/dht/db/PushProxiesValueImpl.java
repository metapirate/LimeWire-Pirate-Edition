/**
 * 
 */
package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteUtils;


public class PushProxiesValueImpl extends AbstractPushProxiesValue {
    
    private static final long serialVersionUID = -2912251955825278890L;

    /**
     * The GUID of the Gnutella Node.
     */
    private final byte[] guid;
    
    /**
     * Gnutella features bit-field.
     */
    private final byte features;
    
    /**
     * Gnutella Firewall-2-Firewall Transfer Protocol version.
     */
    private final int fwtVersion;
    
    /**
     * The port number which may differ from the Contact addresse's
     * port number.
     */
    private final int port;
    
    /**
     * A Set of PushProxy IpPorts.
     */
    private final Set<? extends IpPort> proxies;
    
    /**
     * The raw bytes of the value.
     */
    private final byte[] data;
    
    /**
     * TLS info for the push proxies.
     */
    private final BitNumbers tlsInfo;
    
    /**
     * Constructor for testing purposes.
     */
    public PushProxiesValueImpl(Version version, byte[] guid, 
            byte features, int fwtVersion, 
            int port, Set<? extends IpPort> proxies) {
        super(version);
        
        this.guid = guid;
        this.features = features;
        this.fwtVersion = fwtVersion;
        this.port = port;
        this.proxies = new IpPortSet(proxies);
        this.tlsInfo = AbstractPushProxiesValue.getNumbersFromProxies(proxies);
        this.data = AbstractPushProxiesValue.serialize(this);
    }
    
    public PushProxiesValueImpl(Version version, byte[] data) throws DHTValueException {
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
            
            this.guid = ggep.getBytes(AbstractPushProxiesValue.CLIENT_ID);
            if (guid.length != 16) {
                throw new DHTValueException("Illegal GUID length: " + guid.length);
            }
            
            // Ideally this would be changed to getByte and getByte would be added,
            // but since clients in the field are inserting features as an int,
            // we need to preserve the functionality.
            this.features = (byte)ggep.getInt(AbstractPushProxiesValue.FEATURES);
            this.fwtVersion = ggep.getInt(AbstractPushProxiesValue.FWT_VERSION);
            
            byte[] portBytes = ggep.getBytes(AbstractPushProxiesValue.PORT);
            this.port = ByteUtils.beb2short(portBytes, 0) & 0xFFFF;
            if (!NetworkUtils.isValidPort(port)) {
                throw new DHTValueException("Illegal port: " + port);
            }
            
            BitNumbers tlsInfo = BitNumbers.EMPTY_BN;
            try {
                tlsInfo = new BitNumbers(ggep.getBytes(AbstractPushProxiesValue.TLS));
            } catch (BadGGEPPropertyException notThere){}
            
            byte[] proxiesBytes = ggep.getBytes(AbstractPushProxiesValue.PROXIES);
            ByteArrayInputStream bais = new ByteArrayInputStream(proxiesBytes);
            DataInputStream in = new DataInputStream(bais);
            
            Set<IpPort> proxies = new IpPortSet();
            int id = 0;
            while(in.available() > 0) {
                int length = in.readUnsignedByte();
                
                if (length != 6 && length != 18) {
                    throw new IOException("Illegal IP:Port length: " + length);
                }
                
                byte[] addr = new byte[length-2];
                in.readFully(addr);
                
                int port = in.readUnsignedShort();
                
                if (!NetworkUtils.isValidPort(port)) {
                    throw new DHTValueException("Illegal port: " + port);
                }
                
                IpPort proxy = new IpPortImpl(InetAddress.getByAddress(addr), port);
                if (tlsInfo.isSet(id++))
                    proxy = new ConnectableImpl(proxy, true);
                proxies.add(proxy);
            }
            
            this.proxies = proxies;
            this.tlsInfo = tlsInfo;
            
        } catch (BadGGEPPropertyException err) {
            throw new DHTValueException(err);
            
        } catch (BadGGEPBlockException err) {
            throw new DHTValueException(err);
            
        } catch (UnknownHostException err) {
            throw new DHTValueException(err);
            
        } catch (IOException err) {
            throw new DHTValueException(err);
        }
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

    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getGUID()
     */
    public byte[] getGUID() {
        return guid;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getFeatures()
     */
    public byte getFeatures() {
        return features;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getFwtVersion()
     */
    public int getFwtVersion() {
        return fwtVersion;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getPort()
     */
    public int getPort() {
        return port;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.gnutella.dht.db.PushProxiesValue#getPushProxies()
     */
    public Set<? extends IpPort> getPushProxies() {
        return proxies;
    }
    
    public BitNumbers getTLSInfo() {
        return tlsInfo;
    }
}