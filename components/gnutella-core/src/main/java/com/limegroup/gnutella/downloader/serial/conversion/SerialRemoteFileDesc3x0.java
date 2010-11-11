package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.SerialXml;

class SerialRemoteFileDesc3x0 implements Serializable, SerialRemoteFileDesc {

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
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isTlsCapable()
     */
    public boolean isTlsCapable() {
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getHttpPushAddr()
     */
    public String getHttpPushAddr() {
        return null;
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
        return _size;
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
        return false;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getVendor()
     */
    public String getVendor() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#isHttp11()
     */
    public boolean isHttp11() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.serial.conversion.SerialRemoteFileDesc#getPropertiesMap()
     */
    public Map<String, Serializable> getPropertiesMap() {
        return Collections.emptyMap();
    }
    
    

}
