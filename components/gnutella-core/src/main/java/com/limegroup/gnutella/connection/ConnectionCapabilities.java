package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

/**
 * Defines the interface from which all capabilities of a {@link Connection} can
 * be queried. Capabilities can range from those describes in headers to
 * capabilities as expressed from specific vendor messages.
 * <code>ConnectionCapabilities</code> also contains some methods that combine
 * multiple headers & capabilities in order to return a coherent multi-value
 * result, such as {@link #isOldLimeWire()), {@link #isGoodLeaf()} and others.
 */
public interface ConnectionCapabilities {

    public static enum Capability {
        TLS
    }

    /**
     * Returns the number of intra-Ultrapeer connections this node maintains.
     * 
     * @return the number of intra-Ultrapeer connections this node maintains
     */
    public int getNumIntraUltrapeerConnections();

    /**
     * True if the remote host supports query routing (QRP). This is only
     * meaningful in the context of leaf-ultrapeer relationships.
     */
    public boolean isQueryRoutingEnabled();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int supportsVendorMessage(byte[] vendorID, int selector);

    /**
     * @return whether this connection supports routing of vendor messages (i.e.
     *         will not drop a VM that has ttl <> 1 and hops > 0)
     */
    public boolean supportsVMRouting();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsUDPConnectBack();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsTCPConnectBack();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsUDPRedirect();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsTCPRedirect();

    /**
     * @return -1 if UDP crawling is supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsUDPCrawling();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsHopsFlow();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsPushProxy();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsLeafGuidance();

    /**
     * @return -1 if the message isn't supported, else the version number
     *         supported.
     */
    public int remoteHostSupportsHeaderUpdate();

    /**
     * Return whether or not the remote host supports feature queries.
     */
    public boolean getRemoteHostSupportsFeatureQueries();

    /**
     * @return the maximum selector of capability supported, else -1 if no
     *         support.
     */
    public int getRemoteHostFeatureQuerySelector();

    /**
     * @return true if the capability is supported.
     */
    public boolean remoteHostSupportsWhatIsNew();

    /**
     * Returns the DHT version if the remote host is an active DHT node or -1 if
     * it is not.
     */
    public int remostHostIsActiveDHTNode();

    /**
     * Returns the DHT version if the remote host is a passive DHT node or -1 if
     * it is not.
     * 
     */
    public int remostHostIsPassiveDHTNode();

    /**
     * Returns the DHT version of the remote host is a passive leaf DHT node or
     * -1 if it is not.
     */
    public int remoteHostIsPassiveLeafNode();

    /**
     * Returns the vendor string reported by this connection, i.e., the
     * USER_AGENT property, or null if it wasn't set.
     * 
     * @return the vendor string, or null if unknown
     */
    public String getUserAgent();

    /**
     * Returns whether or not the remote host is a LimeWire (or derivative).
     */
    public boolean isLimeWire();

    /** Returns whether or not this is considered an 'old' LimeWire. */
    public boolean isOldLimeWire();

    public boolean isGoodUltrapeer();

    public boolean isGoodLeaf();

    public boolean supportsPongCaching();

    public boolean isHighDegreeConnection();

    /**
     * Returns whether or not this connection is to an Ultrapeer that supports
     * query routing between Ultrapeers at 1 hop.
     * 
     * @return <tt>true</tt> if this is an Ultrapeer connection that exchanges
     *         query routing tables with other Ultrapeers at 1 hop, otherwise
     *         <tt>false</tt>
     */
    public boolean isUltrapeerQueryRoutingConnection();

    /**
     * Returns whether or not this connections supports "probe" queries, or
     * queries sent at TTL=1 that should not block the send path of subsequent,
     * higher TTL queries.
     * 
     * @return <tt>true</tt> if this connection supports probe queries,
     *         otherwise <tt>false</tt>
     */
    public boolean supportsProbeQueries();

    /**
     * Accessor for whether or not this connection has received any headers.
     * 
     * @return <tt>true</tt> if this connection has finished initializing and
     *         therefore has headers, otherwise <tt>false</tt>
     */
    public boolean receivedHeaders();

    /**
     * Accessor for the <tt>HandshakeResponse</tt> instance containing all of
     * the Gnutella connection headers passed by this node.
     * 
     * @return the <tt>HandshakeResponse</tt> instance containing all of the
     *         Gnutella connection headers passed by this node
     */
    public HandshakeResponse getHeadersRead();

    /**
     * Accessor for the LimeWire version reported in the connection headers for
     * this node.
     */
    public String getVersion();

    /**
     * Returns true iff this connection wrote "Ultrapeer: false". This does NOT
     * necessarily mean the connection is shielded.
     */
    public boolean isLeafConnection();

    /** Returns true iff this connection wrote "Supernode: true". */
    public boolean isSupernodeConnection();

    /**
     * Returns true iff the connection is an Ultrapeer and I am a leaf, i.e., if
     * I wrote "X-Ultrapeer: false", this connection wrote "X-Ultrapeer: true"
     * (not necessarily in that order). <b>Does NOT require that QRP is enabled</b>
     * between the two; the Ultrapeer could be using reflector indexing, for
     * example.
     */
    public boolean isClientSupernodeConnection();

    /**
     * Returns true iff the connection is an Ultrapeer and I am a Ultrapeer, ie:
     * if I wrote "X-Ultrapeer: true", this connection wrote "X-Ultrapeer: true"
     * (not necessarily in that order). <b>Does NOT require that QRP is enabled</b>
     * between the two; the Ultrapeer could be using reflector indexing, for
     * example.
     */
    public boolean isSupernodeSupernodeConnection();

    /**
     * Returns whether or not this connection is to a ultrapeer supporting
     * GUESS.
     * 
     * @return <tt>true</tt> if the node on the other end of this Ultrapeer
     *         connection supports GUESS, <tt>false</tt> otherwise
     */
    public boolean isGUESSUltrapeer();

    /**
     * Returns true iff I am a supernode shielding the given connection, i.e.,
     * if I wrote "X-Ultrapeer: true" and this connection wrote "X-Ultrapeer:
     * false, and <b>both support query routing</b>.
     */
    public boolean isSupernodeClientConnection();

    public void setMessagesSupportedVendorMessage(MessagesSupportedVendorMessage vm);

    public void setCapabilitiesVendorMessage(CapabilitiesVM vm);

    public void setHeadersRead(HandshakeResponse createResponse);

    public void setHeadersWritten(HandshakeResponse writtenHeaders);

    public HandshakeResponse getHeadersWritten();

    public boolean isCapabilitiesVmSet();

    public int getCapability(Capability tls);

    /**
     * 
     * Returns the peer's supported version of the out-of-band proxying control
     * message or -1.
     */
    public int getSupportedOOBProxyControlVersion();
    
    public boolean canAcceptIncomingTCP();
    
    public boolean canDoFWT();

}