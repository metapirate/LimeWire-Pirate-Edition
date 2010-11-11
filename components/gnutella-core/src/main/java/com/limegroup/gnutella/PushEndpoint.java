package com.limegroup.gnutella;

import java.util.Set;

import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.http.HTTPHeaderValue;

/**
 * a class that represents an endpoint behind one or more PushProxies.
 * almost everything is immutable including the contents of the set.
 * 
 * the network format this is serialized to is:
 * byte 0 (from right-to-left): 
 *    - bits 0-2 how many push proxies we have (so max is 7)
 *    - bits 3-4 the version of the f2f transfer protocol this altloc supports
 *    - bits 5-6 other possible features.
 *    - bit  7   set if the TLS-capable push proxy indexes byte is included
 * bytes 1-16 : the guid
 * bytes 17-22: ip:port of the address (if FWT version > 0)
 * followed by a byte of TLS-capable PushProxy indexes (if bit 7 of features is set)
 * followed by 6 bytes per PushProxy 
 * 
 * the http format this is serialized to is an ascii string consisting of
 * ';'-delimited tokens.  The first token is the client GUID represented in hex
 * and is the only required token.  The other tokens can be addresses of push proxies
 * or various feature headers.  At most one of the tokens should be the external ip and port 
 * of the firewalled node in a port:ip format. Currently the only feature header we 
 * parse is the fwawt header that contains the version number of the firewall to 
 * firewall transfer protocol supported by the altloc.  In addition, the 'pptls=' field
 * can indicate which, if any, push proxies support TLS.  If the field is present, it 
 * must be immediately before the listing of the push proxies.  The hexadecimal string
 * after the '=' is a bit-representation of which push proxies are valid for TLS.
 * 
 * A PE does not need to know the actual external address of the firewalled host,
 * however without that knowledge we cannot do firewall-to-firewall transfer with 
 * the given host.  Also, the RemoteFileDesc objects requires a valid IP for construction,
 * so in the case we do not know the external address we return a BOGUS_IP.
 * 
 * Examples:
 * 
 *  //altloc with 2 proxies and supports firewall transfer 1 :
 * 
 * <ThisIsTheGUIDASDF>;fwt/1.0;20.30.40.50:60;1.2.3.4:5567
 * 
 *   //altloc with 1 proxy and doesn't support firewall transfer, with external address:
 * 
 * <ThisIsTHeGUIDasfdaa527>;1.2.3.4:5564;6346:2.3.4.5
 * 
 * //altloc with 1 proxy and supports two features we don't know/care about :
 * 
 * <ThisIsTHeGUIDasfdaa527>;someFeature/3.2;10.20.30.40:5564;otherFeature/0.4
 * 
 * //altloc with 1 proxy (the first) that's TLS capable, 1 that isn't:
 * 
 * <ThisIsTheGUIDASDF>;fwt/1.0;pptls=8;20.30.40.50:60;1.2.3.4:5567
 * 
 *  //altloc without any proxies and doesn't support any features
 *  // not very useful, but still valid  
 * 
 * <ThisIsTheGUIDasdf23457>
 */
public interface PushEndpoint extends HTTPHeaderValue, IpPort, Address {

    public static final int HEADER_SIZE = 17; //guid+# of proxies, maybe other things too

    public static final int PROXY_SIZE = 6; //ip:port

    public static final byte PLAIN = 0x0; //no features for this PE

    public static final byte PPTLS_BINARY = (byte) 0x80;
    
    public static final byte SIZE_MASK=0x7; //0000 0111
    
    public static final byte FWT_VERSION_MASK=0x18; //0001 1000
    
    //the features mask does not clear the bits we do not understand
    //because we may pass on the altloc to someone who does.
    public static final byte FEATURES_MASK= (byte)0xE0;   //1110 0000
    
    /** The pptls portion constant. */
    public static final String PPTLS_HTTP = "pptls";

    /** The maximum number of proxies to use. */
    public static final int MAX_PROXIES = 4;

    /**
     * @return a byte-packed representation of this
     */
    byte[] toBytes(boolean includeTLS);

    /**
     * creates a byte packet representation of this
     * @param where the byte [] to serialize to 
     * @param offset the offset within that byte [] to serialize
     */
    void toBytes(byte[] where, int offset, boolean includeTLS);

    /**
     * Returns the GUID of the client that can be reached through the pushproxies.
     */
    byte[] getClientGUID();

    /**
     * @return a view of the current set of proxies, never returns null
     */
    Set<? extends IpPort> getProxies();

    /**
     * @return which version of F2F transfers this PE supports.
     * This always returns the most current version we know the PE supports
     * unless it has never been put in the map.
     */
    int getFWTVersion();

    /**
     * Should return the {@link GUID#hashCode()} of {@link #getClientGUID()}.
     */
    int hashCode();

    /**
     * Equality should be based on the equality of the value of {@link #getClientGUID()}.
     */
    boolean equals(Object other);
    
    /**
     * @return the various features this PE reports.  This always
     * returns the most current features, or the ones it was created with
     * if they have never been updated.
     */
    byte getFeatures();

    /**
     * @return true if this is the push endpoint for the local node
     */
    boolean isLocal();

    /**
     * Updates either the PushEndpoint or the GUID_PROXY_MAP to ensure
     * that GUID_PROXY_MAP has a reference to all live PE GUIDs and
     * all live PE's reference the same GUID object as in GUID_PROXY_MAP.
     * 
     * If this method is not called, the PE will know only about the set
     * of proxies the remote host had when it was created.  Otherwise it
     * will point to the most recent known set.
     */
    // TODO remove this in the long run and use the cache explicitly
    void updateProxies(boolean good);

    /**
     * Can return null if no valid push endpoint can be cloned. 
     */
    PushEndpoint createClone();
    
    /**
     * Returns an {@link IpPort} representing the valid external address of
     * this push endpoint if it is known, otherwise <code>null</code>. 
     */
    IpPort getValidExternalAddress();
    
    /**
     * @return the external address if known otherwise {@link RemoteFileDesc#BOGUS_IP}
     */
    String getAddress();
}