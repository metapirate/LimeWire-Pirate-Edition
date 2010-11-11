package com.limegroup.gnutella.downloader;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.auth.Credentials;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.net.address.AddressFactory;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.Objects;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A default implementation for {@link RemoteFileDesc}.
 */
public class RemoteFileDescImpl implements RemoteFileDesc {

    @SuppressWarnings("unused")
    private static final Log LOG = LogFactory.getLog(RemoteFileDesc.class);

    private final String _filename;

    private final long _index;

    private final byte[] _clientGUID;
    
    private final byte[] queryGUID;

    private final int _speed;

    private final int _quality;

    private final boolean _replyToMulticast;

    private final LimeXMLDocument _xmlDoc;

    private final Set<URN> _urns;
    
    /**
     * Boolean indicating whether or not the remote host has browse host
     * enabled.
     */
    private final boolean _browseHostEnabled;

    private final String _vendor;

    public static final String TYPE = "RFD";

    /**
     * Whether or not the remote host supports HTTP/1.1 This is purposely NOT
     * IMMUTABLE. Before we connect, we can only assume the remote host supports
     * HTTP/1.1 by looking at the set of URNs. If any exist, we assume HTTP/1.1
     * is supported (because URNs were added to Gnutella after HTTP/1.1). Once
     * we connect, this value is set to be whatever the host reports in the
     * response line.
     * <p>
     * When deserializing, this value may be wrong for older download.dat files.
     * (Older versions will always set this to false, because the field did not
     * exist.) To counter that, when deserializing, if this is false, we set it
     * to true if any URNs are present.
     */
    private boolean _http11;

    /** True if this host is TLS capable. */
    private boolean _tlsCapable;

    /**
     * The cached hash code for this RFD.
     */
    private int _hashCode = 0;

    /**
     * The creation time of this file.
     */
    private final long _creationTime;

    /**
     * the spam rating of this rfd.
     */
    private volatile float _spamRating = 0.f;

    /** the security of this RemoteFileDesc. */
    private Status _secureStatus = Status.INSECURE;

    private final long _size;

    private final Address address;

    private final AddressFactory addressFactory;

    public RemoteFileDescImpl(Address address, long index, String filename, long size,
            byte[] clientGUID, int speed, int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor, long createTime,
            boolean http11, AddressFactory addressFactory, byte[] queryGUID) {
        this.addressFactory = addressFactory;
        this.address = Objects.nonNull(address, "address");
        if ((speed & 0xFFFFFFFF00000000L) != 0)
            throw new IllegalArgumentException("invalid speed: " + speed);
        if (filename.equals(""))
            throw new IllegalArgumentException("cannot accept empty string file name");
        if (size < 0 || size > MAX_FILE_SIZE)
            throw new IllegalArgumentException("invalid size: " + size);
        if ((index & 0xFFFFFFFF00000000L) != 0)
            throw new IllegalArgumentException("invalid index: " + index);

        _speed = speed;
        _index = index;
        _filename = filename;
        _size = size;
        _clientGUID = clientGUID;
        _quality = quality;
        _browseHostEnabled = browseHost;
        _replyToMulticast = replyToMulticast;
        _vendor = vendor;
        _creationTime = createTime;
        _xmlDoc = xmlDoc;
        _http11 = http11;
        _urns = UrnSet.resolve(urns);
        this.queryGUID = queryGUID;
    }

    /** Returns true if the host supports TLS. */
    public boolean isTLSCapable() {
        return _tlsCapable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#isHTTP11()
     */
    public boolean isHTTP11() {
        return _http11;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#setHTTP11(boolean)
     */
    public void setHTTP11(boolean http11) {
        _http11 = http11;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#isMe(byte[])
     */
    public boolean isMe(byte[] myClientGUID) {
        return Arrays.equals(_clientGUID, myClientGUID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#isFromAlternateLocation()
     */
    public boolean isFromAlternateLocation() {
        return "ALT".equals(_vendor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getCreationTime()
     */
    public long getCreationTime() {
        return _creationTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getIndex()
     */
    public final long getIndex() {
        return _index;
    }

    public final long getSize() {
        return _size;
    }

    /**
     * Accessor for the file name for this file, which can be <tt>null</tt>.
     * 
     * @return the file name for this file, which can be <tt>null</tt>
     */
    public final String getFileName() {
        return _filename;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getClientGUID()
     */
    public final byte[] getClientGUID() {
        return _clientGUID;
    }
    
    @Override
    public final byte[] getQueryGUID() {
        return queryGUID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getSpeed()
     */
    public final int getSpeed() {
        return _speed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getVendor()
     */
    public final String getVendor() {
        return _vendor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#isBrowseHostEnabled()
     */
    public final boolean isBrowseHostEnabled() {
        return _browseHostEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getQuality()
     */
    public final int getQuality() {
        return _quality;
    }

    /**
     * Returns the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>,
     * which can be <tt>null</tt>.
     * 
     * @return the <tt>LimeXMLDocument</tt> for this <tt>RemoteFileDesc</tt>,
     *         which can be <tt>null</tt>.
     */
    public final LimeXMLDocument getXMLDocument() {
        return _xmlDoc;
    }

    /**
     * Accessor for the <tt>Set</tt> of URNs for this <tt>RemoteFileDesc</tt>.
     * 
     * @return the <tt>Set</tt> of URNs for this <tt>RemoteFileDesc</tt>
     */
    public final Set<URN> getUrns() {
        return _urns;
    }

    /**
     * Accessor for the SHA1 URN for this <tt>RemoteFileDesc</tt>.
     * 
     * @return the SHA1 <tt>URN</tt> for this <tt>RemoteFileDesc</tt>, or
     *         <tt>null</tt> if there is none
     */
    public final URN getSHA1Urn() {
        return UrnSet.getSha1(_urns);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getUrl()
     */
    public String getUrlPath() {
        URN urn = getSHA1Urn();
        if (urn == null) {
            return "/get/" + _index + "/" + _filename;
        } else {
            return HTTPConstants.URI_RES_N2R + urn.httpStringValue();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#isReplyToMulticast()
     */
    public final boolean isReplyToMulticast() {
        return _replyToMulticast;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#isAltLocCapable()
     */
    public final boolean isAltLocCapable() {
        if (address instanceof PushEndpoint) {
            if (((PushEndpoint) address).getProxies().isEmpty()) {
                return false;
            }
        }
        if (getSHA1Urn() != null && !_replyToMulticast) {
            return true;
        }
        return false;
    }

    /**
     * Overrides <tt>Object.equals</tt> to return instance equality based on the
     * equality of all <tt>RemoteFileDesc</tt> fields.
     * 
     * @return <tt>true</tt> if all of fields of this <tt>RemoteFileDesc</tt>
     *         instance are equal to all of the fields of the specified object,
     *         and <tt>false</tt> if this is not the case, or if the specified
     *         object is not a <tt>RemoteFileDesc</tt>.
     * 
     *         Dynamic values such as _http11, and _availableSources are not
     *         checked here, as they can change and still be considered the same
     *         "remote file".
     * 
     *         The _host field may be equal for many firewalled locations;
     *         therefore it is necessary that we distinguish those by their
     *         client GUIDs
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RemoteFileDesc))
            return false;
        RemoteFileDesc other = (RemoteFileDesc) o;
        if (!address.equals(other.getAddress())) {
            return false;
        }

        if (_size != other.getSize())
            return false;

        if ((_clientGUID == null) != (other.getClientGUID() == null))
            return false;

        if (_clientGUID != null && !(Arrays.equals(_clientGUID, other.getClientGUID())))
            return false;

        if (_urns.isEmpty() && other.getUrns().isEmpty())
            return Objects.equalOrNull(_filename, other.getFileName());
        else
            return _urns.equals(other.getUrns());
    }

    /**
     * Overrides the hashCode method of Object to meet the contract of hashCode.
     * Since we override equals, it is necessary to also override hashcode to
     * ensure that two "equal" RemoteFileDescs return the same hashCode, less we
     * unleash unknown havoc on the hash-based collections.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        if (_hashCode == 0) {
            int result = 17;
            result = (37 * result) + address.hashCode();
            result = (int) ((37 * result) + _size);
            result = (37 * result) + _urns.hashCode();
            if (_clientGUID != null)
                result = (37 * result) + (new GUID(_clientGUID)).hashCode();
            _hashCode = result;
        }
        return _hashCode;
    }

    @Override
    public String toString() {
        return address.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#setSpamRating(float)
     */
    public void setSpamRating(float rating) {
        _spamRating = rating;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getSpamRating()
     */
    public float getSpamRating() {
        return _spamRating;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#getSecureStatus()
     */
    public Status getSecureStatus() {
        return _secureStatus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#setSecureStatus(int)
     */
    public void setSecureStatus(Status secureStatus) {
        this._secureStatus = secureStatus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.RemoteFileDesc#toMemento()
     */
    public RemoteHostMemento toMemento() {
        return new RemoteHostMemento(address, _filename, _index, _clientGUID, _speed, _size,
                _quality, _replyToMulticast, xmlString(), _urns, _browseHostEnabled, _vendor,
                _http11, TYPE, addressFactory);
    }

    private String xmlString() {
        if (_xmlDoc == null)
            return null;
        else
            return _xmlDoc.getXMLString();
    }

    @Override
    public boolean isSpam() {
        return getSpamRating() >= SearchSettings.FILTER_SPAM_RESULTS.getValue();
    }

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public Credentials getCredentials() {
        return null;
    }

    @Override
    public boolean matchesQuery(String query) {
        return _xmlDoc != null || QueryUtils.filenameMatchesQuery(_filename, query);
    }
}
