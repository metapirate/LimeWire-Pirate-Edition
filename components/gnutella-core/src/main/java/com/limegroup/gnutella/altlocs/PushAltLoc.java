package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.limewire.io.GUID;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.http.HTTPConstants;

/**
 * A firewalled altloc.
 */
public class PushAltLoc extends AbstractAlternateLocation {

    /**
     * the host we would send push to. Null if not firewalled.
     */
    private final PushEndpoint _pushAddress;

    private final ApplicationServices applicationServices;

    /**
     * Creates a new AlternateLocation for a firewalled host.
     * 
     * @throws IOException
     */
    protected PushAltLoc(final PushEndpoint address, final URN sha1,
            ApplicationServices applicationServices) {
        super(sha1);

        if (address == null)
            throw new NullPointerException("null address");

        _pushAddress = address;
        this.applicationServices = applicationServices;
    }

    @Override
    protected String generateHTTPString() {
        return _pushAddress.httpStringValue();
    }

    @Override
    public RemoteFileDesc createRemoteFileDesc(long size,
            RemoteFileDescFactory remoteFileDescFactory) {
        Set<URN> urnSet = new UrnSet(getSHA1Urn());
        int quality = 3;

        RemoteFileDesc ret = remoteFileDescFactory.createRemoteFileDesc(_pushAddress, 0,
                HTTPConstants.URI_RES_N2R + SHA1_URN, size, _pushAddress.getClientGUID(), 1000,
                quality, false, null, urnSet, false, ALT_VENDOR, -1);

        return ret;
    }

    @Override
    public synchronized AlternateLocation createClone() {
        PushAltLoc ret = new PushAltLoc(_pushAddress.createClone(), SHA1_URN, applicationServices);
        ret._count = this._count;
        return ret;
    }

    @Override
    public boolean isMe() {
        return Arrays.equals(_pushAddress.getClientGUID(), applicationServices.getMyGUID());
    }

    /**
     * Updates the proxies in this PushEndpoint. If this method is called, the
     * PE of this PushLoc will always point to the current set of proxies we
     * know the remote host has. Otherwise, the PE will point to the set of
     * proxies we knew the host had when it was created.
     * <p>
     * Note: it is a really good idea to call this method before adding this
     * pushloc to a AlternateLocationCollection which may already contain a
     * pushloc for the same host.
     */
    public void updateProxies(boolean isGood) {
        _pushAddress.updateProxies(isGood);
    }

    /**
     * @return the PushAddress.
     */
    public PushEndpoint getPushAddress() {
        return _pushAddress;
    }

    /**
     * @return the Firewall transfer protocol version this altloc supports. 0 if
     *         its not supported.
     */
    public int supportsFWTVersion() {
        return _pushAddress.getFWTVersion();
    }

    // stubbed out -- no demotion or promotion for push locs.
    @Override
    void promote() {
    }

    // stubbed out -- no demotion or promotion for push locs.
    @Override
    void demote() {
    }

    /**
     * PushLocs are considered demoted once all their proxies are empty. This
     * ensures that the PE will stay alive until it has exhausted all possible
     * proxies.
     */
    @Override
    public boolean isDemoted() {
        return _pushAddress.getProxies().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof PushAltLoc))
            return false;

        if (!super.equals(o)) {
            return false;
        }
        PushAltLoc other = (PushAltLoc) o;
        return _pushAddress.equals(other._pushAddress);
    }

    @Override
    public int compareTo(AlternateLocation obj) {

        if (this == obj) // equal
            return 0;

        int ret = super.compareTo(obj);

        if (ret != 0)
            return ret;
        if (!(obj instanceof PushAltLoc))
            return 1;

        PushAltLoc pal = (PushAltLoc) obj;

        return GUID.GUID_BYTE_COMPARATOR.compare(_pushAddress.getClientGUID(), pal.getPushAddress()
                .getClientGUID());

    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = super.hashCode();
            result = (37 * result) + this._pushAddress.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    /**
     * Overrides toString to return a string representation of this
     * <tt>AlternateLocation</tt>, namely the pushAddress and the date.
     * 
     * @return the string representation of this alternate location
     */
    @Override
    public String toString() {
        return _pushAddress + "," + _count;
    }
}
