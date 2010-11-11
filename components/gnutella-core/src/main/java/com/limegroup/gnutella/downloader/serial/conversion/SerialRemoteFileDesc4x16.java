package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.limewire.util.GenericsUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.xml.SerialXml;

class SerialRemoteFileDesc4x16 implements Serializable, SerialRemoteFileDesc {

    private static final long serialVersionUID = 6619479308616716538L;

    private String _host;

    private int _port;

    private String _filename;

    private long _index;

    private byte[] _clientGUID;

    private int _speed;

    private int _size;

    private boolean _chatEnabled;

    private int _quality;

    private boolean _replyToMulticast;

    private SerialXml[] _xmlDocs;

    private Set<URN> _urns;

    private boolean _browseHostEnabled;

    private boolean _firewalled;

    private String _vendor;

    private boolean _http11;
    
    private Map<String, Serializable> propertiesMap;
    
    private transient long longSize;
    private transient boolean tlsCapable;
    private transient String httpPushAddr;

    private void writeObject(ObjectOutputStream output) throws IOException {}
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (_urns != null) {
            Set<URN> newUrns = GenericsUtils.scanForSet(_urns, URN.class,
                    GenericsUtils.ScanMode.NEW_COPY_REMOVED, UrnSet.class);
            if (_urns != newUrns)
                _urns = Collections.unmodifiableSet(newUrns);
        }
        
        // if we saved any properties, read them now
        if (propertiesMap != null) {
            Boolean tlsBoolean = (Boolean)propertiesMap.get("CONNECT_TYPE");
            if(tlsBoolean != null)
                tlsCapable = tlsBoolean.booleanValue();
            
            String http = (String)propertiesMap.get("PUSH_ADDR");
            // try the older serialized name if it didn't have the newer one.
            if(http == null)
                http = (String)propertiesMap.get("_pushAddr");
            
            Long size64 = (Long)propertiesMap.get("LONG_SIZE");
            if (size64 == null)
                longSize = _size;
            else
                longSize = size64.longValue();
        } else {
            // very old format, make sure we get the size right
            longSize = _size;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isTlsCapable()
     */
    public boolean isTlsCapable() {
        return tlsCapable;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getHttpPushAddr()
     */
    public String getHttpPushAddr() {
        return httpPushAddr;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getHost()
     */
    public String getHost() {
        return _host;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getPort()
     */
    public int getPort() {
        return _port;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getFilename()
     */
    public String getFilename() {
        return _filename;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getIndex()
     */
    public long getIndex() {
        return _index;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getClientGUID()
     */
    public byte[] getClientGUID() {
        return _clientGUID;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getSpeed()
     */
    public int getSpeed() {
        return _speed;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getSize()
     */
    public long getSize() {
        return longSize;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isChatEnabled()
     */
    public boolean isChatEnabled() {
        return _chatEnabled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getQuality()
     */
    public int getQuality() {
        return _quality;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isReplyToMulticast()
     */
    public boolean isReplyToMulticast() {
        return _replyToMulticast;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getXml()
     */
    public String getXml() {
        return _xmlDocs != null && _xmlDocs.length > 0 ? _xmlDocs[0].getXml(false) : null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getUrns()
     */
    public Set<URN> getUrns() {
        return _urns;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isBrowseHostEnabled()
     */
    public boolean isBrowseHostEnabled() {
        return _browseHostEnabled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isFirewalled()
     */
    public boolean isFirewalled() {
        return _firewalled;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getVendor()
     */
    public String getVendor() {
        return _vendor;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isHttp11()
     */
    public boolean isHttp11() {
        return _http11;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getPropertiesMap()
     */
    public Map<String, Serializable> getPropertiesMap() {
        return propertiesMap;
    }
    
    

}
