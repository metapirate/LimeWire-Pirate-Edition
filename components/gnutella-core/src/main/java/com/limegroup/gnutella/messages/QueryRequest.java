package com.limegroup.gnutella.messages;

import java.util.Set;

import org.limewire.security.AddressSecurityToken;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface QueryRequest extends Message {

    /**
     * The string used by Clip2 reflectors to index hosts.
     * <p>
     * Deprecated: queries are not sent out with this and LimeWire will
     * respond with an empty result set.
     */
    public static final String INDEXING_QUERY = "    ";

    /**
     * The string used by LimeWire to browse hosts.
     * <p>
     * Deprecated: queries are not sent out with this and LimeWire will
     * respond with an empty result set.
     */
    public static final String BROWSE_QUERY = "*.*";
    
    /**
     * Constant for the default query TTL.
     */
    public static final byte DEFAULT_TTL = 6;
    
    /**
     * The meaningless query string we put in URN queries. Needed because
     * LimeWire's drop empty queries....
     */
    static final String DEFAULT_URN_QUERY = "\\";
    
    
    // these specs may seem backwards, but they are not - ByteOrder.short2leb
    // puts the low-order byte first, so over the network 0x0080 would look
    // like 0x8000
    public static final int SPECIAL_MINSPEED_MASK = 0x0080;

    public static final int SPECIAL_FIREWALL_MASK = 0x0040;

    public static final int SPECIAL_XML_MASK = 0x0020;

    public static final int SPECIAL_OUTOFBAND_MASK = 0x0004;

    public static final int SPECIAL_FWTRANS_MASK = 0x0002;

    /** Mask for audio queries - input 0 | AUDIO_MASK | .... to specify
     *  audio responses.
     */
    public static final int AUDIO_MASK = 0x0004;

    /** Mask for video queries - input 0 | VIDEO_MASK | .... to specify
     *  video responses.
     */
    public static final int VIDEO_MASK = 0x0008;

    /** Mask for document queries - input 0 | DOC_MASK | .... to specify
     *  document responses.
     */
    public static final int DOC_MASK = 0x0010;

    /** Mask for image queries - input 0 | IMAGE_MASK | .... to specify
     *  image responses.
     */
    public static final int IMAGE_MASK = 0x0020;

    /** Mask for windows programs/packages queries - input 0 | WIN_PROG_MASK
     *  | .... to specify windows programs/packages responses.
     */
    public static final int WIN_PROG_MASK = 0x0040;

    /** Mask for linux/osx programs/packages queries - input 0 | LIN_PROG_MASK
     *  | .... to specify linux/osx programs/packages responses.
     */
    public static final int LIN_PROG_MASK = 0x0080;
    
    /**
     * Mask for torrent queries.
     */
    public static final int TORRENT_MASK = 0x00100;

    public static final String WHAT_IS_NEW_QUERY_STRING = "WhatIsNewXOXO";

    /**
     * Accessor for the payload of the query hit.
     *
     * @return the query hit payload
     */
    public byte[] getPayload();

    /** 
     * Returns the query string of this message.<p>
     *
     * The caller should not call the getBytes() method on the returned value,
     * as this seems to cause problems on the Japanese Macintosh.  If you need
     * the raw bytes of the query string, call getQueryByteAt(int).
     */
    public String getQuery();

    /**
     * @return the rich query LimeXMLDocument
     */
    public LimeXMLDocument getRichQuery();
    
    /**
     * @return null if there is none
     */
    public String getRichQueryString();

    /**
     * Returns the <tt>Set</tt> of <tt>URN</tt> instances for this query.
     *
     * @return  the <tt>Set</tt> of <tt>URN</tt> instances for this query, which
     * may be empty (not null) if no URNs were requested
     */
    public Set<URN> getQueryUrns();

    /**
     * Returns whether or not this query contains URNs.
     *
     * @return <tt>true</tt> if this query contains URNs,<tt>false</tt> otherwise
     */
    public boolean hasQueryUrns();

    /**
     * Note: the minimum speed can be represented as a 2-byte unsigned
     * number, but Java shorts are signed.  Hence we must use an int.  The
     * value returned is always smaller than 2^16.
     */
    public int getMinSpeed();

    /**
     * Returns true if the query source is a firewalled servent.
     */
    public boolean isFirewalledSource();

    /**
     * Returns true if the query source desires Lime meta-data in responses.
     */
    public boolean desiresXMLResponses();

    /**
     * Returns true if the query source can do a firewalled transfer.
     */
    public boolean canDoFirewalledTransfer();

    /**
     * Returns true if the query source can accept out-of-band replies for
     * any supported protocol version.
     * <p>
     * Use getReplyAddress() and getReplyPort() if this is true to know where to
     * it. Always send XML if you are sending an out-of-band reply.
     */
    public boolean desiresOutOfBandReplies();

    /**
     * Returns true if sender desires out-of-band replies for protocol version
     * 2.
     */
    public boolean desiresOutOfBandRepliesV2();

    /**
     * Returns true if sender desires out-of-band replies for protocol version
     * 3.
     */
    public boolean desiresOutOfBandRepliesV3();

    /**
     * Returns true if the query source does not want you to proxy for it.
     */
    public boolean doNotProxy();

    /**
     * Returns true if this query is for 'What is new?' content, i.e. usually
     * the top 3 YOUNGEST files in your library.
     */
    public boolean isWhatIsNewRequest();

    /**
     * Returns true if this is a feature query.
     */
    public boolean isFeatureQuery();

    /**
     * @return whether this is a browse host query
     */
    public boolean isBrowseHostQuery();

    /**
     * Returns 0 if this is not a "feature" query, else it returns the selector
     * of the feature query, e.g. What Is New returns 1.
     */
    public int getFeatureSelector();

    /**
     * Returns true if the query request has a security token request,
     * this implies the sender requests OOB replies, protocol version 3.
     */
    public boolean isSecurityTokenRequired();
    
    /**
     * @return true if the query desires results for partial files
     */
    public boolean desiresPartialResults();

    /** Returns the address to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    public String getReplyAddress();

    /** Returns true if the input bytes match the OOB address of this query.
     */
    public boolean matchesReplyAddress(byte[] ip);

    /** Returns the port to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    public int getReplyPort();

    /**
     * Accessor for whether or not this is a requery from a LimeWire.
     *
     * @return <tt>true</tt> if it is an automated requery from a LimeWire,
     *  otherwise <tt>false</tt>
     */
    public boolean isLimeRequery();

    /**
     * @return true if this is likely a query for LimeWire
     */
    public boolean isQueryForLW();

    /**
     * Returns the AddressSecurityToken associated with this Request.  May very well be
     * null.  Usually only UDP QueryRequests will have non-null QueryKeys.
     */
    public AddressSecurityToken getQueryKey();

    /** @return true if the query has no constraints on the type of results
     *  it wants back.
     */
    public boolean desiresAll();

    /** @return true if the query desires 'Audio' results back.
     */
    public boolean desiresAudio();

    /** @return true if the query desires 'Video' results back.
     */
    public boolean desiresVideo();

    /** @return true if the query desires 'Document' results back.
     */
    public boolean desiresDocuments();

    /** @return true if the query desires 'Image' results back.
     */
    public boolean desiresImages();

    /** @return true if the query desires 'Programs/Packages' for Windows
     *  results back.
     */
    public boolean desiresWindowsPrograms();

    /** @return true if the query desires 'Programs/Packages' for Linux/OSX
     *  results back.
     */
    public boolean desiresLinuxOSXPrograms();
    
    /** @return true if the query desires Torrents.
     */
    public boolean desiresTorrents();

    /**
     * Returns the mask of allowed programs.
     */
    public int getMetaMask();

    /** Marks this as being created by us. */
    public void originate();

    /** Determines if this query was created by us. */
    public boolean isOriginated();
    
    /**
     * @return whether or not a response to this query should include XML.
     */
    public boolean shouldIncludeXMLInResponse();
    
    /**
     * @return true if the requestor is interested in the non-metadata
     * sha1 urn of files in {@link Response responses}
     */
    public boolean desiresNMS1Urn();
}