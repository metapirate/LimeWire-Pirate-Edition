package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.security.SecureMessage;

import com.limegroup.gnutella.Response;

public interface QueryReply extends Message, SecureMessage {

    public static final int TRUE=1;
    public static final int FALSE=0;
    public static final int UNDEFINED=-1;
    
    // some parameters about xml, namely the max size of a xml collection string.
    public static final int XML_MAX_SIZE = 32768;

    /** 2 bytes for public area, 2 bytes for xml length. */
    public static final int COMMON_PAYLOAD_LEN = 4;

    public void setOOBAddress(InetAddress addr, int port);

    /**
     * Sets the guid for this message. Is needed, when we want to cache 
     * query replies or for some other reason want to change the GUID as 
     * per the guid of query request.
     * @param guid the guid to be set
     */
    public void setGUID(GUID guid);

    // inherit doc comment
    public void writePayload(OutputStream out) throws IOException;

    /**
     * Sets this reply to be considered a 'browse host' reply.
     */
    public void setBrowseHostReply(boolean isBH);

    /**
     * Gets whether or not this reply is from a browse host request.
     */
    public boolean isBrowseHostReply();

    /** Return the associated xml metadata string if the query reply
     *  contained one.
     */
    public byte[] getXMLBytes();

    /** Return the number of results N in this query. */
    public short getResultCount();

    /** 
     * @return the number of unique results (per SHA1) carried in this message
     */
    public short getUniqueResultCount();
    
    /**
     * @return the number of unique partial results carried in this message
     * <= getResultCount()
     */
    public short getPartialResultCount();

    public int getPort();

    /** Returns the IP address of the responding host in standard
     *  dotted-decimal format, e.g., "192.168.0.1". */
    public String getIP();

    /**
     * Accessor the IP address in byte array form.
     *
     * @return the IP address for this query hit as an array of bytes
     */
    public byte[] getIPBytes();

    public int getSpeed();

    /**
     * Returns the Response[].  Throws BadPacketException if this
     * data couldn't be extracted.
     */
    public Response[] getResultsArray() throws BadPacketException;

    /** Returns an iterator that will yield the results, each as an
     *  instance of the Response class.  Throws BadPacketException if
     *  this data couldn't be extracted.  */
    public Iterator<Response> getResults() throws BadPacketException;

    /** Returns a List that will yield the results, each as an
     *  instance of the Response class.  Throws BadPacketException if
     *  this data couldn't be extracted.  */
    public List<Response> getResultsAsList() throws BadPacketException;

    /**
     * Returns the name of this' vendor, all capitalized.  Returns
     * the empty String if the data couldn't be extracted, either because it
     * is missing or corrupted.
     */
    public String getVendor();

    /** 
     * Returns true if this's push flag is set, i.e., a push download is needed.
     * Returns false if the flag is present but not set.  Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolean getNeedsPush() throws BadPacketException;

    /** 
     * Returns true if this has no more download slots.  Returns false if the
     * busy bit is present but not set.  Throws BadPacketException if the flag
     * couldn't be extracted, either because it is missing or corrupted.  
     */
    public boolean getIsBusy() throws BadPacketException;

    /** 
     * Returns true if this has successfully uploaded a complete file (bit set).
     * Returns false if the bit is not set.  Throws BadPacketException if the
     * flag couldn't be extracted, either because it is missing or corrupted.  
     */
    public boolean getHadSuccessfulUpload() throws BadPacketException;

    /** 
     * Returns true if the speed in this QueryReply was measured (bit set).
     * Returns false if it was set by the user (bit unset).  Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.  
     */
    public boolean getIsMeasuredSpeed() throws BadPacketException;

    /** Returns true iff this client supports TLS. */
    public boolean isTLSCapable();


    /** @return true if the remote host can firewalled transfers.
     */
    public boolean getSupportsFWTransfer();

    /** @return 1 or greater if FW Transfer is supported, else 0.
     */
    public byte getFWTransferVersion();

    /** 
     * Returns true iff the client supports browse host feature.
     */
    public boolean getSupportsBrowseHost();

    /** 
     * Returns true iff the reply was sent in response to a multicast query.
     * @return true, iff the reply was sent in response to a multicast query,
     * false otherwise
     * @exception Throws BadPacketException if
     * the flag couldn't be extracted, either because it is missing or
     * corrupted.  Typically this exception is treated the same way as returning
     * false. 
     */
    public boolean isReplyToMulticastQuery();

    /** Sets whether or not this reply is allowed to have an MCAST field. */
    public void setMulticastAllowed(boolean allowed);

    /** Returns true if this reply tried to fake an MCAST field. */
    public boolean isFakeMulticast();

    /**
     * @return null or a non-zero length array of PushProxy hosts.
     */
    public Set<? extends IpPort> getPushProxies();

    /**
     * Returns the message authentication bytes that were sent along with
     * this query reply or null the none have been sent.
     */
    public byte[] getSecurityToken();

    /**
     * Determines if this result has secure data.
     * This does NOT determine if the result has been verified
     * as secure.
     */
    public boolean hasSecureData();

    /** Returns the 16 byte client ID (i.e., the "footer") of the
     *  responding host.  */
    public byte[] getClientGUID();

    /**
     * This method calculates the quality of service for a given host.  The
     * calculation is some function of whether or not the host is busy, whether
     * or not the host has ever received an incoming connection, etc.
     * <p>
     * Moved this code from SearchView to here permanently, so we avoid
     * duplication.  It makes sense from a data point of view, but this method
     * isn't really essential an essential method.
     *
     * @return a int from -1 to 3, with -1 for "never work" and 3 for "always
     * work".  Typically a return value of N means N+1 stars will be displayed
     * in the GUI. */
    public int calculateQualityOfService();

    public byte[] getPayload();
    
    /**
     * @return if this reply is created locally. false means from network.
     */
    public boolean isLocal();

    public boolean isFirewalled();

    /**
     * Parses the message packet and throws a <code>BadPacketException</code> if the
     * packet was invalid.
     * @throws BadPacketException
     */
    void validate() throws BadPacketException;
    
    /**
     * Parses and returns the GGEP extension block attached to this QueryReply.
     * Returns null if there is no GGEP extension block, or if the block exists
     * but cannot be parsed. 
     */
    public GGEP getGGEP();
}