package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

/**
 * A {@link ConnectionCapabilities} that delegates to {@link HandshakeResponse}
 * objects to keep track of what headers were read or written, and
 * {@link CapabilitiesVM} and {@link MessagesSupportedVendorMessage} objects to
 * keep track of what vendor messages & capabilities are supported. 
 */
public class ConnectionCapabilitiesImpl implements ConnectionCapabilities {

    private volatile CapabilitiesVM capabilitiesVendorMessage;

    // start with empty responses
    private volatile HandshakeResponse headersRead = HandshakeResponse.createEmptyResponse();

    private volatile HandshakeResponse headersWritten = HandshakeResponse.createEmptyResponse();

    private volatile MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#getUserAgent()
     */
    public String getUserAgent() {
        return headersRead.getUserAgent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isLimeWire()
     */
    public boolean isLimeWire() {
        return headersRead.isLimeWire();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isOldLimeWire()
     */
    public boolean isOldLimeWire() {
        return headersRead.isOldLimeWire();
    }

    // inherit doc comment
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isGoodUltrapeer()
     */
    public boolean isGoodUltrapeer() {
        return headersRead.isGoodUltrapeer();
    }

    // inherit doc comment
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isGoodLeaf()
     */
    public boolean isGoodLeaf() {
        return headersRead.isGoodLeaf();
    }

    // inherit doc comment
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#supportsPongCaching()
     */
    public boolean supportsPongCaching() {
        return headersRead.supportsPongCaching();
    }

    /**
     * Returns the number of intra-Ultrapeer connections this node maintains.
     * 
     * @return the number of intra-Ultrapeer connections this node maintains
     */
    public int getNumIntraUltrapeerConnections() {
        return headersRead.getNumIntraUltrapeerConnections();
    }

    // implements ReplyHandler interface -- inherit doc comment
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isHighDegreeConnection()
     */
    public boolean isHighDegreeConnection() {
        return headersRead.isHighDegreeConnection();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isUltrapeerQueryRoutingConnection()
     */
    public boolean isUltrapeerQueryRoutingConnection() {
        return headersRead.isUltrapeerQueryRoutingConnection();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#supportsProbeQueries()
     */
    public boolean supportsProbeQueries() {
        return headersRead.supportsProbeQueries();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#receivedHeaders()
     */
    public boolean receivedHeaders() {
        return headersRead != HandshakeResponse.createEmptyResponse();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#headers()
     */
    public HandshakeResponse getHeadersRead() {
        return headersRead;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#getVersion()
     */
    public String getVersion() {
        return headersRead.getVersion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isLeafConnection()
     */
    public boolean isLeafConnection() {
        return headersRead.isLeaf();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isSupernodeConnection()
     */
    public boolean isSupernodeConnection() {
        return headersRead.isUltrapeer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isClientSupernodeConnection()
     */
    public boolean isClientSupernodeConnection() {
        // Is remote host a supernode...
        if (!isSupernodeConnection())
            return false;

        // ...and am I a leaf node?
        String value = headersWritten.props().getProperty(HeaderNames.X_ULTRAPEER);
        if (value == null)
            return false;
        else
            return !Boolean.valueOf(value).booleanValue();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isSupernodeSupernodeConnection()
     */
    public boolean isSupernodeSupernodeConnection() {
        // Is remote host a supernode...
        if (!isSupernodeConnection())
            return false;

        // ...and am I a leaf node?
        String value = headersWritten.props().getProperty(HeaderNames.X_ULTRAPEER);
        if (value == null)
            return false;
        else
            return Boolean.valueOf(value).booleanValue();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isGUESSUltrapeer()
     */
    public boolean isGUESSUltrapeer() {
        return headersRead.isGUESSUltrapeer();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#isSupernodeClientConnection()
     */
    public boolean isSupernodeClientConnection() {
        // Is remote host a supernode...
        if (!isLeafConnection())
            return false;

        // ...and am I a supernode?
        String value = headersWritten.props().getProperty(HeaderNames.X_ULTRAPEER);
        if (value == null)
            return false;
        else if (!Boolean.valueOf(value).booleanValue())
            return false;

        // ...and do both support QRP?
        return isQueryRoutingEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#supportsGGEP()
     */
    @Deprecated
    // used only in tests!
    public boolean supportsGGEP() {
        return headersRead.supportsGGEP();
    }

    /**
     * True if the remote host supports query routing (QRP). This is only
     * meaningful in the context of leaf-ultrapeer relationships.
     */
    public boolean isQueryRoutingEnabled() {
        return headersRead.isQueryRoutingEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#supportsVendorMessage(byte[],
     *      int)
     */
    public int supportsVendorMessage(byte[] vendorID, int selector) {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsMessage(vendorID, selector);
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#supportsVMRouting()
     */
    public boolean supportsVMRouting() {
        if (headersRead != null)
            return headersRead.supportsVendorMessages() >= 0.2;
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsUDPConnectBack()
     */
    public int remoteHostSupportsUDPConnectBack() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsUDPConnectBack();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsTCPConnectBack()
     */
    public int remoteHostSupportsTCPConnectBack() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsTCPConnectBack();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsUDPRedirect()
     */
    public int remoteHostSupportsUDPRedirect() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsUDPConnectBackRedirect();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsTCPRedirect()
     */
    public int remoteHostSupportsTCPRedirect() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsTCPConnectBackRedirect();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsUDPCrawling()
     */
    public int remoteHostSupportsUDPCrawling() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsUDPCrawling();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsHopsFlow()
     */
    public int remoteHostSupportsHopsFlow() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsHopsFlow();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsPushProxy()
     */
    public int remoteHostSupportsPushProxy() {
        if ((messagesSupportedVendorMessage != null) && isClientSupernodeConnection())
            return messagesSupportedVendorMessage.supportsPushProxy();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsLeafGuidance()
     */
    public int remoteHostSupportsLeafGuidance() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsLeafGuidance();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsHeaderUpdate()
     */
    public int remoteHostSupportsHeaderUpdate() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsHeaderUpdate();
        return -1;
    }

    /**
     * Returns the peer's supported version of the out-of-band proxying control
     * message or -1.
     */
    public int getSupportedOOBProxyControlVersion() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsOOBProxyingControl();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsInspections()
     */
    public int remoteHostSupportsInspections() {
        if (messagesSupportedVendorMessage != null)
            return messagesSupportedVendorMessage.supportsInspectionRequests();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#getRemoteHostSupportsFeatureQueries()
     */
    public boolean getRemoteHostSupportsFeatureQueries() {
        if (capabilitiesVendorMessage != null)
            return capabilitiesVendorMessage.supportsFeatureQueries() > 0;
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#getRemoteHostFeatureQuerySelector()
     */
    public int getRemoteHostFeatureQuerySelector() {
        if (capabilitiesVendorMessage != null)
            return capabilitiesVendorMessage.supportsFeatureQueries();
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostSupportsWhatIsNew()
     */
    public boolean remoteHostSupportsWhatIsNew() {
        if (capabilitiesVendorMessage != null)
            return capabilitiesVendorMessage.supportsWhatIsNew();
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remostHostIsActiveDHTNode()
     */
    public int remostHostIsActiveDHTNode() {
        if (capabilitiesVendorMessage != null) {
            return capabilitiesVendorMessage.isActiveDHTNode();
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remostHostIsPassiveDHTNode()
     */
    public int remostHostIsPassiveDHTNode() {
        if (capabilitiesVendorMessage != null) {
            return capabilitiesVendorMessage.isPassiveDHTNode();
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionCapabilities#remoteHostIsPassiveLeafNode()
     */
    public int remoteHostIsPassiveLeafNode() {
        if (capabilitiesVendorMessage != null) {
            return capabilitiesVendorMessage.isPassiveLeafNode();
        }
        return -1;
    }

    public int getCapability(Capability capability) {
        if (capabilitiesVendorMessage != null) {
            switch (capability) {
            case TLS:
                return capabilitiesVendorMessage.supportsTLS();
            default:
                throw new IllegalArgumentException("unknown capability!");
            }
        } else {
            return -1;
        }
    }

    public HandshakeResponse getHeadersWritten() {
        return headersWritten;
    }

    public boolean isCapabilitiesVmSet() {
        return capabilitiesVendorMessage != null;
    }

    public void setCapabilitiesVendorMessage(CapabilitiesVM vm) {
        this.capabilitiesVendorMessage = vm;
    }

    public void setHeadersRead(HandshakeResponse createResponse) {
        this.headersRead = createResponse;
    }

    public void setHeadersWritten(HandshakeResponse writtenHeaders) {
        this.headersWritten = writtenHeaders;
    }

    public void setMessagesSupportedVendorMessage(MessagesSupportedVendorMessage vm) {
        this.messagesSupportedVendorMessage = vm;
    }

    public boolean canAcceptIncomingTCP() {
        if (capabilitiesVendorMessage == null)
            return true;
        return capabilitiesVendorMessage.canAcceptIncomingTCP();
    }
    
    public boolean canDoFWT() {
        if (capabilitiesVendorMessage == null)
            return true;
        return capabilitiesVendorMessage.canDoFWT();
    }
}
