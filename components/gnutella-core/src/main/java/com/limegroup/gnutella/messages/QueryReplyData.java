package com.limegroup.gnutella.messages;


import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.util.DataUtils;

class QueryReplyData {

    /** If parsed, the responses vendor string, if defined, or null otherwise. */
    private volatile String vendor = null;
    
    /** If parsed, one of TRUE (push needed), FALSE, or UNDEFINED. */
    private volatile int pushFlag = QueryReply.UNDEFINED;
    
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int busyFlag = QueryReply.UNDEFINED;
    
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int uploadedFlag = QueryReply.UNDEFINED;
    
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int measuredSpeedFlag = QueryReply.UNDEFINED;

    /** Determines if the remote host supports chat */
    private volatile boolean supportsChat = false;
    
    /** Determines if the remote host supports browse host */
    private volatile boolean supportsBrowseHost = false;
    
    /** Determines if this is a reply to a multicast query */
    private volatile boolean replyToMulticast = false;
    
    /** Determines if the remote host supports FW transfers */
    private volatile boolean supportsFWTransfer = false;
    
    /** Version number of FW Transfer the host supports. */
    private volatile byte fwTransferVersion = (byte)0;
    
    /** If parsed, the response records for this, or null if they could not be parsed. */
    private volatile Response[] responses = null;
    
    /** The number of unique results (by SHA1) this message carries */
    private volatile short uniqueResultURNs;
    
    /** 
     * The number of unique results (by SHA1) for partial files this message carries.
     * INVARIANT: <= uniqueResultURNS;
     */
    private volatile short partialResultCount;

    /** the PushProxy info for this hit. */ 
    private volatile Set<? extends IpPort> proxies; 
    
    /** Whether or not this is a result from a browse-host reply. */  
    private volatile boolean browseHostReply;  
    
    /** The data with info about the secure result. */  
    private volatile SecureGGEPData secureGGEP;
    
    /** The xml chunk that contains metadata about xml responses*/  
    private volatile byte[] xmlBytes = DataUtils.EMPTY_BYTE_ARRAY;
    
    /** A secure token, if any */
    private volatile byte[] securityToken = null;
    
    /** Whether or not this QueryReply supports TLS. */
    private volatile boolean supportsTLS;
    
    /** Offset of the start of a GGEP block contained in this reply */
    private volatile int ggepStart = -1;
    
    /** Offset of the end of a GGEP block contained in this reply */
    private volatile int ggepEnd = -1;
    
    /** Offset of the control flag in the QHD */
    private volatile int qhdOffset = -1;
    
    public int getBusyFlag() {
        return busyFlag;
    }

    public void setBusyFlag(int busyFlag) {
        this.busyFlag = busyFlag;
    }

    public byte getFwTransferVersion() {
        return fwTransferVersion;
    }

    public void setFwTransferVersion(byte fwTransferVersion) {
        this.fwTransferVersion = fwTransferVersion;
    }

    public int getMeasuredSpeedFlag() {
        return measuredSpeedFlag;
    }

    public void setMeasuredSpeedFlag(int measuredSpeedFlag) {
        this.measuredSpeedFlag = measuredSpeedFlag;
    }

    public int getPushFlag() {
        return pushFlag;
    }

    public void setPushFlag(int pushFlag) {
        this.pushFlag = pushFlag;
    }

    public boolean isReplyToMulticast() {
        return replyToMulticast;
    }

    public void setReplyToMulticast(boolean replyToMulticast) {
        this.replyToMulticast = replyToMulticast;
    }

    public Response[] getResponses() {
        return responses;
    }

    public void setResponses(Response[] responses) {
        this.responses = responses;
    }

    public boolean isSupportsBrowseHost() {
        return supportsBrowseHost;
    }

    public void setSupportsBrowseHost(boolean supportsBrowseHost) {
        this.supportsBrowseHost = supportsBrowseHost;
    }

    public boolean isSupportsChat() {
        return supportsChat;
    }

    public void setSupportsChat(boolean supportsChat) {
        this.supportsChat = supportsChat;
    }

    public boolean isSupportsFWTransfer() {
        return supportsFWTransfer;
    }

    public void setSupportsFWTransfer(boolean supportsFWTransfer) {
        this.supportsFWTransfer = supportsFWTransfer;
    }

    public short getUniqueResultURNs() {
        return uniqueResultURNs;
    }

    public void setUniqueResultURNs(short uniqueResultURNs) {
        this.uniqueResultURNs = uniqueResultURNs;
    }
    
    public short getPartialResultCount() {
        return partialResultCount;
    }
    
    public void setPartialResultCount(short partialResultCount) {
        this.partialResultCount = partialResultCount;
    }

    public int getUploadedFlag() {
        return uploadedFlag;
    }

    public void setUploadedFlag(int uploadedFlag) {
        this.uploadedFlag = uploadedFlag;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public boolean isBrowseHostReply() {
        return browseHostReply;
    }

    public void setBrowseHostReply(boolean browseHostReply) {
        this.browseHostReply = browseHostReply;
    }

    public Set<? extends IpPort> getProxies() {
        return proxies;
    }

    public void setProxies(Set<? extends IpPort> proxies) {
        this.proxies = proxies;
    }

    public SecureGGEPData getSecureGGEP() {
        return secureGGEP;
    }

    public void setSecureGGEP(SecureGGEPData secureGGEP) {
        this.secureGGEP = secureGGEP;
    }

    public byte[] getXmlBytes() {
        return xmlBytes;
    }

    public void setXmlBytes(byte[] xmlBytes) {
        this.xmlBytes = xmlBytes;
    }
    
    public void setSecurityToken(byte[] securityToken) {
        this.securityToken = securityToken;
    }
    
    public byte[] getSecurityToken() {
        return securityToken;
    }
    
    public void setTLSCapable(boolean capable) {
        supportsTLS = capable;
    }
    
    public boolean isTLSCapable() {
        return supportsTLS;
    }
    
    public void setGGEPStart(int offset) {
        this.ggepStart = offset;
    }
    
    public int getGGEPStart() {
        return ggepStart;
    }
    
    public void setGGEPEnd(int offset) {
        this.ggepEnd = offset;
    }
    
    public int getGGEPEnd() {
        return ggepEnd;
    }
    
    public void setQHDOffset(int offset) {
        qhdOffset = offset;
    }
    
    public int getQHDOffset() {
        return qhdOffset;
    }
}
