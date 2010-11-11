package org.limewire.core.impl.friend;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.limewire.core.settings.SearchSettings;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.net.address.AddressFactory;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class FriendRemoteFileDesc implements RemoteFileDesc {

    static final String TYPE = "XMPPRFD";

    private final FriendAddress address;
    private final long index;
    private final String filename;
    private final long size;
    private final byte[] clientGUID;
    private final int speed;
    private final int quality;
    private final LimeXMLDocument xmlDoc;
    private final Set<URN> urns;
    private final String vendor;
    private final long createTime;
    private final AddressFactory addressFactory; 
    private boolean http11;
    private final FriendAddressResolver addressResolver;
    private int hashCode = -1;
    private float spamRating = 0;
    
    public FriendRemoteFileDesc(FriendAddress address, long index, String filename,
            long size, byte[] clientGUID, int speed, int quality, LimeXMLDocument xmlDoc, Set<? extends URN> urns,
            String vendor, long createTime, boolean http11,
            AddressFactory addressFactory, FriendAddressResolver addressResolver) {
        this.address = address;
        this.index = index;
        this.filename = filename;
        this.size = size;
        this.clientGUID = Objects.nonNull(clientGUID, "clientGUID");
        this.speed = speed;
        this.quality = quality;
        this.xmlDoc = xmlDoc;
        this.urns = Collections.unmodifiableSet(urns);
        this.vendor = vendor;
        this.createTime = createTime;
        this.http11 = http11;
        this.addressResolver = addressResolver;
        this.addressFactory = addressFactory;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FriendRemoteFileDesc)) {
            return false;
        }
        FriendRemoteFileDesc other = (FriendRemoteFileDesc)obj;
        if (!Arrays.equals(clientGUID, other.clientGUID)) {
            return false;
        }
        if (!address.equals(other.address)) {
            return false;
        }
        if (size != other.size) {
            return false;
        }
        if (!urns.equals(other.urns)) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int h = hashCode;
        if (h == -1) {
            h = address.hashCode();
            h = 31 * h + (int)size;
            h = 31 * h + new GUID(clientGUID).hashCode();
            h = 31 * h + urns.hashCode();
            hashCode = h;
        }
        return h;
    }
    
    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public Credentials getCredentials() {
        FriendPresence presence = addressResolver.getPresence(address);
        if (presence == null) {
            return null;
        }
        // TODO change auth token to type string
        AuthTokenFeature authTokenFeature = (AuthTokenFeature)presence.getFeature(AuthTokenFeature.ID);
        if(authTokenFeature == null) {
            return null;
        }
        return new UsernamePasswordCredentials(presence.getFriend().getNetwork().getCanonicalizedLocalID(), StringUtils.toUTF8String(authTokenFeature.getFeature().getToken()));
    }

    @Override
    public Status getSecureStatus() {
        return Status.INSECURE;
    }

    @Override
    public float getSpamRating() {
        return spamRating;
    }

    @Override
    public String getUrlPath() {
        URN sha1Urn = getSHA1Urn();
        FriendPresence presence = addressResolver.getPresence(address);
        if (presence == null) {
            // race condition, friend is already offline, just return a possibly invalid path
            // download will fail elsewhere
            return HTTPConstants.URI_RES_N2R + sha1Urn.httpStringValue();
        }
        try {
            return CoreGlueFriendService.FRIEND_DOWNLOAD_PREFIX + URLEncoder.encode(presence.getFriend().getNetwork().getCanonicalizedLocalID(), "UTF-8") + HTTPConstants.URI_RES_N2R + sha1Urn.httpStringValue();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAltLocCapable() {
        return false;
    }

    @Override
    public boolean isFromAlternateLocation() {
        return false;
    }

    @Override
    public boolean isMe(byte[] myClientGUID) {
        return Arrays.equals(clientGUID, myClientGUID);
    }

    @Override
    public boolean isSpam() {
        return getSpamRating() >= SearchSettings.FILTER_SPAM_RESULTS.getValue();
    }

    @Override
    public void setSecureStatus(Status secureStatus) {
    }

    @Override
    public void setSpamRating(float rating) {
        spamRating = rating;
    }

    @Override
    public RemoteHostMemento toMemento() {
        return new RemoteHostMemento(address, filename, index, clientGUID, speed, size, quality, isReplyToMulticast(), getXml(), urns, isBrowseHostEnabled(), vendor, http11, TYPE, addressFactory);
    }
    
    private String getXml() {
        return xmlDoc != null ? xmlDoc.getXMLString() : null;
    }

    @Override
    public long getCreationTime() {
        return createTime;
    }

    @Override
    public String getFileName() {
        return filename;
    }

    @Override
    public long getIndex() {
        return index;
    }

    @Override
    public URN getSHA1Urn() {
        URN sha1Urn = UrnSet.getSha1(urns);
        if (sha1Urn == null) {
            throw new IllegalArgumentException(urns + " should have sha1");
        }
        return sha1Urn;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public Set<URN> getUrns() {
        return urns;
    }

    @Override
    public LimeXMLDocument getXMLDocument() {
        return xmlDoc;
    }

    @Override
    public byte[] getClientGUID() {
        return clientGUID;
    }
    
    @Override
    public byte[] getQueryGUID() {
        return null;
    }

    @Override
    public int getQuality() {
        return quality;
    }

    @Override
    public int getSpeed() {
        return speed;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public boolean isBrowseHostEnabled() {
        return true;
    }
 
    @Override
    public boolean isHTTP11() {
        return http11;
    }

    @Override
    public boolean isReplyToMulticast() {
        return false;
    }

    @Override
    public void setHTTP11(boolean http11) {
        this.http11 = http11;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this, filename, address);
    }

    @Override
    public boolean matchesQuery(String query) {
        return xmlDoc != null || QueryUtils.filenameMatchesQuery(filename, query);
    }
}
