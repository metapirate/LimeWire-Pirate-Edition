package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.messages.HUGEExtension.GGEPBlock;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

/**
 * This class creates Gnutella query messages, either from scratch, or
 * from data read from the network.  Queries can contain query strings, 
 * XML query strings, URNs, etc.  The minimum speed field is now used
 * for bit flags to indicate such things as the firewalled status of
 * the querier.<p>
 * 
 * This class also has factory constructors for requeries originated
 * from this LimeWire.  These requeries have specially marked GUIDs
 * that allow us to identify them as requeries.
 */
public class QueryRequestImpl extends AbstractMessage implements QueryRequest {

    private static final Log LOG = LogFactory.getLog(QueryRequestImpl.class);
    
    /**
     * The payload for the query -- includes the query string, the
     * XML query, any URNs, GGEP, etc.
     */
    private final byte[] PAYLOAD;

    /**
     * The "min speed" field.  This was originally used to specify
     * a minimum speed for returned results, but it was never really
     * used this way.  As of LimeWire 3.0 (02/2003), the bits of 
     * this field were changed to specify things like the firewall
     * status of the querier.
     */
    private final int MIN_SPEED;

    /**
     * The query string.
     */
    private final String QUERY;
    
    /**
     * The LimeXMLDocument of the rich query.
     */
    private final LimeXMLDocument XML_DOC;

    /**
     * The feature that this query is.
     */
    private int _featureSelector = 0;
    
    private boolean _isSecurityTokenRequired;
    
    /** If the query desires partial results */
    private boolean _partialResultsDesired;
    
    private boolean _desiresNMS1Urns;
    
    /**
     * Whether or not the GGEP header for Do Not Proxy was found and its
     * field is empty.
     */
    private boolean _doNotProxy = false;
    
    // HUGE v0.93 fields
    /** 
	 * Specific URNs requested.
	 */
    private final Set<URN> QUERY_URNS;

    /**
     * The Query Key associated with this query -- can be null.
     */
    private final AddressSecurityToken QUERY_KEY;

    /**
     * The flag in the 'M' GGEP extension - if non-null, the query is requesting
     * only certain types.
     */
    private Integer _metaMask = null;
    
    /**
     * If we're re-originated this query for a leaf.  This can be set/read
     * after creation.
     */
    private boolean originated = false;

	/**
	 * Cached hash code for this instance.
	 */
	private volatile int _hashCode = 0;
    
	/**
     * Cached illegal characters in search strings.
     */
    private static final char[] ILLEGAL_CHARS =
        SearchSettings.ILLEGAL_CHARS.get();


    /**
     * Cache the maximum length for queries, in bytes.
     */
    private static final int MAX_QUERY_LENGTH =
        SearchSettings.MAX_QUERY_LENGTH.getValue();

    /**
     * Cache the maximum length for XML queries, in bytes.
     */
    private static final int MAX_XML_QUERY_LENGTH =
        SearchSettings.MAX_XML_QUERY_LENGTH.getValue();

    /**
     * Cache the max length for query string message field
     */
    private static final int OLD_LW_MAX_QUERY_FIELD_LEN = 30;
 
    
    /** Constructs a query. */
    QueryRequestImpl(byte[] guid, byte ttl, int minSpeed,
                        String query, String richQuery, 
                        Set<? extends URN> queryUrns,
                        AddressSecurityToken addressSecurityToken, boolean isFirewalled, 
                        Network network, boolean canReceiveOutOfBandReplies,
                        int featureSelector, boolean doNotProxy,
                        int metaFlagMask, boolean normalize,
                        boolean canDoFWT,
                        LimeXMLDocumentFactory limeXMLDocumentFactory) {
        // don't worry about getting the length right at first
        super(guid, Message.F_QUERY, ttl, /* hops */ (byte)0, /* length */ 0, 
              network);
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Creating query request, OOB capable " +
                    canReceiveOutOfBandReplies + ", do not proxy " +
                    doNotProxy);
        }
        
        // make sure the query is normalized.
        // (this may have been normalized elsewhere, but it's okay to do it again)
        if(normalize && query != null)
            query = I18NConvert.instance().getNorm(query);
        
        if((query == null || query.length() == 0) &&
           (richQuery == null || richQuery.length() == 0) &&
           (queryUrns == null || queryUrns.size() == 0)) {
            throw new IllegalArgumentException("cannot create empty query");
        }

        if(query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("query too big: " + query);
        }        

        if(richQuery != null && richQuery.length() > MAX_XML_QUERY_LENGTH) {
            throw new IllegalArgumentException("xml too big: " + richQuery);
        }

        if(query != null && 
          !(queryUrns != null && queryUrns.size() > 0 &&
            query.equals(QueryRequestImpl.DEFAULT_URN_QUERY))
           && hasIllegalChars(query)) {
            throw new IllegalArgumentException("illegal chars: " + query);
        }

        if (featureSelector < 0)
            throw new IllegalArgumentException("Bad feature = " +
                                               featureSelector);
        _featureSelector = featureSelector;
        //NOTE: 504 is the maximum value here since it is the sum of all flags minus
        // the smallest flag. If all flags are set we should not send a flag at all
        // and perform an all request
        if ((metaFlagMask > 0) && (metaFlagMask < 4) || (metaFlagMask > 504))
            throw new IllegalArgumentException("Bad Meta Flag = " +
                                               metaFlagMask);
        if (metaFlagMask > 0)
            _metaMask = metaFlagMask;

        // only set the minspeed if none was input...x
        if (minSpeed == 0) {
            // the new Min Speed format - looks reversed but
            // it isn't because of ByteOrder.short2leb
            minSpeed = SPECIAL_MINSPEED_MASK; 
            // set the firewall bit if i'm firewalled
            if (isFirewalled && !isMulticast())
                minSpeed |= SPECIAL_FIREWALL_MASK;
            // if i'm firewalled and can do solicited, mark the query for fw
            // transfer capability.
            if (isFirewalled && canDoFWT)
                minSpeed |= SPECIAL_FWTRANS_MASK;
            // THE DEAL:
            // if we can NOT receive out of band replies, we want in-band XML -
            // so set the correct bit.
            // if we can receive out of band replies, we do not want in-band XML
            // we'll hope the out-of-band reply guys will provide us all
            // necessary XML.
            
            if(!canReceiveOutOfBandReplies) {
                LOG.debug("Can't receive OOB replies, setting XML flag");
                minSpeed |= SPECIAL_XML_MASK;
            } else if(!SearchSettings.DISABLE_OOB_V2.getBoolean()) {
                LOG.debug("Setting OOBv2 flag");
                minSpeed |= SPECIAL_OUTOFBAND_MASK;
            }
        }

        MIN_SPEED = minSpeed;
        if (query == null) {
            this.QUERY = "";
        } else {
            this.QUERY = query;
        }
        if (richQuery == null || richQuery.equals("")) {
            this.XML_DOC = null;
        } else {
            LimeXMLDocument doc = null;
            try {
                doc = limeXMLDocumentFactory.createLimeXMLDocument(richQuery);
            } catch (SAXException ignored) {
            } catch (SchemaNotFoundException ignored) {
            } catch (IOException ignored) {
            }
            this.XML_DOC = doc;
        }

        Set<URN> tempQueryUrns = null;
        if (queryUrns != null) {
            tempQueryUrns = new UrnSet(queryUrns);
        } else {
            tempQueryUrns = URN.NO_URN_SET;
        }

        this.QUERY_KEY = addressSecurityToken;
        this._doNotProxy = doNotProxy;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteUtils.short2leb((short) MIN_SPEED, baos); // write minspeed
            baos.write(getQueryFieldValue().getBytes("UTF-8")); // write query
            baos.write(0); // null

            // now write any & all HUGE v0.93 General Extension Mechanism
            // extensions

            // this specifies whether or not the extension was successfully
            // written, meaning that the HUGE GEM delimiter should be
            // written before the next extension
            boolean addDelimiterBefore = false;

            byte[] richQueryBytes = null;
            if (XML_DOC != null) {
                assert richQuery != null;
                richQueryBytes = richQuery.getBytes("UTF-8");
            }

            // add the rich query
            addDelimiterBefore = writeGemExtension(baos, addDelimiterBefore, richQueryBytes);

            // add the urns
            addDelimiterBefore = writeGemExtensions(baos, addDelimiterBefore,
                    tempQueryUrns == null ? null : tempQueryUrns.iterator());

            // add the GGEP Extension, if necessary....
            // *----------------------------
            // construct the GGEP block
            GGEP ggepBlock = new GGEP(true); // do COBS

            // add the query key?
            if (this.QUERY_KEY != null) {
                // get query key in byte form....
                ByteArrayOutputStream qkBytes = new ByteArrayOutputStream();
                this.QUERY_KEY.write(qkBytes);
                ggepBlock.put(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT, qkBytes.toByteArray());
            }

            // add the What Is header
            if (_featureSelector > 0)
                ggepBlock.put(GGEPKeys.GGEP_HEADER_FEATURE_QUERY, _featureSelector);

            // add a GGEP-block if we shouldn't proxy
            if (_doNotProxy) {
                LOG.debug("Adding do not proxy OOB header");
                ggepBlock.put(GGEPKeys.GGEP_HEADER_NO_PROXY);
            }

            // add a meta flag
            if (_metaMask != null)
                ggepBlock.put(GGEPKeys.GGEP_HEADER_META, _metaMask.intValue());

            // mark oob query to require support of security tokens
            if (canReceiveOutOfBandReplies) {
                LOG.debug("Adding secure OOB header");
                _isSecurityTokenRequired = true;
                ggepBlock.put(GGEPKeys.GGEP_HEADER_SECURE_OOB);
            }
            
            if (SearchSettings.desiresPartialResults()) {
                _partialResultsDesired = true;
                ggepBlock.put(GGEPKeys.GGEP_HEADER_PARTIAL_RESULT_PREFIX);
            }
            
            if (SearchSettings.DESIRES_NMS1_URNS.getValue()) {
                _desiresNMS1Urns = true;
                ggepBlock.put(GGEPKeys.GGEP_HEADER_NMS1);
            }

            if (QUERY.length() > OLD_LW_MAX_QUERY_FIELD_LEN) {
                ggepBlock.put(GGEPKeys.GGEP_HEADER_EXTENDED_QUERY, QUERY);
            }
            
            // if there are GGEP headers, write them out...
            if (!ggepBlock.isEmpty()) {
                ByteArrayOutputStream ggepBytes = new ByteArrayOutputStream();
                ggepBlock.write(ggepBytes);
                // write out GGEP
                addDelimiterBefore = writeGemExtension(baos, addDelimiterBefore,
                                                       ggepBytes.toByteArray());
            }
            // ----------------------------*

            baos.write(0);                             // final null
        } 
        catch(UnsupportedEncodingException uee) {
            //this should never happen from the getBytes("UTF-8") call
            //but there are UnsupportedEncodingExceptions being reported
            //with UTF-8.
            //Is there other information we want to pass in as the message?
            throw new IllegalArgumentException("could not get UTF-8 bytes for query :"
                                               + QUERY 
                                               + " with richquery :"
                                               + richQuery);
        }
        catch (IOException e) {
            ErrorService.error(e);
        }

        PAYLOAD = baos.toByteArray();
        updateLength(PAYLOAD.length);

        this.QUERY_URNS = Collections.unmodifiableSet(tempQueryUrns);
    }

    /**
     * Generate query string field based on query string ({@link #QUERY} value
     * <p>
     * Assumption:
     * {@link #QUERY} is already set prior to this method getting called
     *
     * @return String representing the query field in the message
     */
    private String getQueryFieldValue() {
        
        // extract keywords from query
        Set<String> keywords = QueryUtils.extractKeywords(QUERY, true);

        // conditions for which the query field will be identical to the query string
        if ((QUERY.length() <= OLD_LW_MAX_QUERY_FIELD_LEN) || (keywords.isEmpty())) {
            return QUERY;
        }

        // adding keywords that fit when appended to query string field, skipping keywords that do not fit.
        return QueryUtils.constructQueryStringFromKeywords(OLD_LW_MAX_QUERY_FIELD_LEN, keywords);
    }


    /**
     * Build a new query with data snatched from network.
     *
     * @param guid the message guid
     * @param ttl the time to live of the query
     * @param hops the hops of the query
     * @param payload the query payload, containing the query string and any
     *  extension strings
     * @param network the network that this query came from.
     * @throws <tt>BadPacketException</tt> if this is not a valid query
     */
    QueryRequestImpl(byte[] guid, byte ttl, byte hops, byte[] payload, Network network,
            LimeXMLDocumentFactory limeXMLDocumentFactory, MACCalculatorRepositoryManager manager)
            throws BadPacketException {
        super(guid, Message.F_QUERY, ttl, hops, payload.length, network);
        PAYLOAD = payload;

        QueryRequestPayloadParser parser = new QueryRequestPayloadParser(payload, manager);

        QUERY = parser.query;

        LimeXMLDocument tempDoc = null;
        try {
            tempDoc = limeXMLDocumentFactory.createLimeXMLDocument(parser.richQuery);
        } catch (SAXException ignored) {
        } catch (SchemaNotFoundException ignored) {
        } catch(IOException ignored) {
        }
        this.XML_DOC = tempDoc;
        MIN_SPEED = parser.minSpeed;

        _featureSelector = parser.featureSelector;

        _doNotProxy = parser.doNotProxy;
        
        _metaMask = parser.metaMask;
        
        _isSecurityTokenRequired = parser.hasSecurityTokenRequest;
        
        _partialResultsDesired = parser.partialResultsDesired;
        
        _desiresNMS1Urns = parser.desiresNMS1Urns;

        if (parser.queryUrns == null) {
            QUERY_URNS = Collections.emptySet();
        } else {
            QUERY_URNS = Collections.unmodifiableSet(parser.queryUrns);
        }
        QUERY_KEY = parser.addressSecurityToken;
        if (QUERY.length() == 0 && parser.richQuery.length() == 0 && QUERY_URNS.size() == 0) {
            throw new BadPacketException("empty query");
        }
        if (QUERY.length() > MAX_QUERY_LENGTH) {
            // throw BadPacketException.QUERY_TOO_BIG;
            throw new BadPacketException("query too big: " + QUERY);
        }        

        if(parser.richQuery.length() > MAX_XML_QUERY_LENGTH) {
            //throw BadPacketException.XML_QUERY_TOO_BIG;
            throw new BadPacketException("xml too big: " + parser.richQuery);
        }

        if(!(QUERY_URNS.size() > 0 && QUERY.equals(QueryRequestImpl.DEFAULT_URN_QUERY))
           && hasIllegalChars(QUERY)) {
            //throw BadPacketException.ILLEGAL_CHAR_IN_QUERY;
            throw new BadPacketException("illegal chars: " + QUERY);
        }
    }
    
    private static boolean hasIllegalChars(String query) {
        return StringUtils.containsCharacters(query,ILLEGAL_CHARS);
    }

    /**
     * Returns a new GUID appropriate for query requests.  If isRequery,
     * the GUID query is marked.
     */
    public static byte[] newQueryGUID(boolean isRequery) {
        if (isRequery)
            return GUID.makeGuidRequery();
        byte [] ret = GUID.makeGuid();
        if (MessageSettings.STAMP_QUERIES.getValue())
            GUID.timeStampGuid(ret);
        return ret;
    }

    @Override
    protected void writePayload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
    }

    /**
     * Accessor for the payload of the query hit.
     *
     * @return the query hit payload
     */
    public byte[] getPayload() {
        return PAYLOAD;
    }

    /** 
     * Returns the query string of this message.<p>
     *
     * The caller should not call the getBytes() method on the returned value,
     * as this seems to cause problems on the Japanese Macintosh.  If you need
     * the raw bytes of the query string, call getQueryByteAt(int).
     */
    public String getQuery() {
        return QUERY;
    }
    
    /**
     * Returns the rich query LimeXMLDocument.
     *
     * @return the rich query LimeXMLDocument
     */
    public LimeXMLDocument getRichQuery() {
        return XML_DOC;
    }
    
    /**
     * Helper method used internally for getting the rich query string.
     */
    public String getRichQueryString() {
        if( XML_DOC == null )
            return null;
        else
            return XML_DOC.getXMLString();
    }       
 
    /**
     * Returns the <tt>Set</tt> of <tt>URN</tt> instances for this query.
     *
     * @return  the <tt>Set</tt> of <tt>URN</tt> instances for this query, which
     * may be empty (not null) if no URNs were requested
     */
    public Set<URN> getQueryUrns() {
        return QUERY_URNS;
    }

    /**
     * Returns whether or not this query contains URNs.
     * 
     * @return <tt>true</tt> if this query contains URNs,<tt>false</tt>
     *         otherwise
     */
    public boolean hasQueryUrns() {
        return !QUERY_URNS.isEmpty();
    }

    /**
     * Note: the minimum speed can be represented as a 2-byte unsigned number,
     * but Java shorts are signed. Hence we must use an int. The value returned
     * is always smaller than 2^16.
     */
    public int getMinSpeed() {
        return MIN_SPEED;
    }

    /**
     * Returns true if the query source is a firewalled servent.
     */
    public boolean isFirewalledSource() {
        if ( !isMulticast() ) {
            if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
                if ((MIN_SPEED & SPECIAL_FIREWALL_MASK) > 0)
                    return true;
            }
        }
        return false;
    }
 
 
    /**
     * Returns true if the query source desires Lime meta-data in responses.
     */
    public boolean desiresXMLResponses() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_XML_MASK) > 0)
                return true;
        }
        return false;        
    }


    /**
     * Returns true if the query source can do a firewalled transfer.
     */
    public boolean canDoFirewalledTransfer() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_FWTRANS_MASK) > 0)
                return true;
        }
        return false;        
    }


    /**
     * Returns true if the query source can accept out-of-band replies for
     * any supported protocol version.
     * <p>
     * Use getReplyAddress() and getReplyPort() if this is true to know where to
     * it. Always send XML if you are sending an out-of-band reply.
     */
    public boolean desiresOutOfBandReplies() {
        return desiresOutOfBandRepliesV2() || desiresOutOfBandRepliesV3();
    }
    
    /**
     * Returns true if sender desires out-of-band replies for protocol version
     * 2.
     */
    public boolean desiresOutOfBandRepliesV2() {
        if ((MIN_SPEED & SPECIAL_MINSPEED_MASK) > 0) {
            if ((MIN_SPEED & SPECIAL_OUTOFBAND_MASK) > 0)
                return true;
        }
        return false;
    }
    
    /**
     * Returns true if sender desires out-of-band replies for protocol version
     * 3.
     */
    public boolean desiresOutOfBandRepliesV3() {
        return isSecurityTokenRequired(); 
    }
    
    /**
     * Returns true if the query source does not want you to proxy for it.
     */
    public boolean doNotProxy() {
        return _doNotProxy;
    }
    
    /**
     * Returns true if this query is for 'What is new?' content, i.e. usually
     * the top 3 YOUNGEST files in your library.
     */
    public boolean isWhatIsNewRequest() {
        return _featureSelector == FeatureSearchData.WHAT_IS_NEW;
    }
    
    /**
     * Returns true if this is a feature query.
     */
    public boolean isFeatureQuery() {
        return _featureSelector > 0;
    }
    
    /**
     * @return whether this is a browse host query
     */
    public boolean isBrowseHostQuery() {
        return INDEXING_QUERY.equals(getQuery());
    }

    /**
     * Returns 0 if this is not a "feature" query, else it returns the selector
     * of the feature query, e.g. What Is New returns 1.
     */
    public int getFeatureSelector() {
        return _featureSelector;
    }
    
    /**
     * Returns true if the query request has a security token request,
     * this implies the sender requests OOB replies, protocol version 3.
     */
    public boolean isSecurityTokenRequired() {
        return _isSecurityTokenRequired;
    }

    public boolean desiresPartialResults() {
        return _partialResultsDesired;
    }
    
    @Override
    public boolean desiresNMS1Urn() {
        return _desiresNMS1Urns;
    }
    
    /** Returns the address to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    public String getReplyAddress() {
        return (new GUID(getGUID())).getIP();
    }

        
    /** Returns true if the input bytes match the OOB address of this query.
     */
    public boolean matchesReplyAddress(byte[] ip) {
        return (new GUID(getGUID())).matchesIP(ip);
    }

        
    /** Returns the port to send a out-of-band reply to.  Only useful
     *  when desiresOutOfBandReplies() == true.
     */
    public int getReplyPort() {
        return (new GUID(getGUID())).getPort();
    }

    /**
     * Accessor for whether or not this is a requery from a LimeWire.
     * 
     * @return <tt>true</tt> if it is an automated requery from a LimeWire,
     *         otherwise <tt>false</tt>
     */
    public boolean isLimeRequery() {
        return GUID.isLimeRequeryGUID(getGUID());
    }

    /**
     * @return true if this is likely a query for LimeWire.
     */
    public boolean isQueryForLW() {
        for (String term : SearchSettings.LIME_SEARCH_TERMS.get()) {
            if (getQuery().length() > 0 &&
                    getQuery().toLowerCase(Locale.US).contains(term))
                return true;

            if (getRichQuery() != null) {
                for (String keyword : getRichQuery().getKeyWords())
                    if (keyword.toLowerCase(Locale.US).contains(term))
                        return true;
            }
        }
        return false;
    }
        
    /**
     * Returns the AddressSecurityToken associated with this Request.  May very well be
     * null.  Usually only UDP QueryRequests will have non-null QueryKeys.
     */
    public AddressSecurityToken getQueryKey() {
        return QUERY_KEY;
    }

    /** @return true if the query has no constraints on the type of results
     *  it wants back.
     */
    public boolean desiresAll() {
        return (_metaMask == null);
    }

    /** @return true if the query desires 'Audio' results back.
     */
    public boolean desiresAudio() {
        return hasMetaMask(AUDIO_MASK);
    }
    
    /** @return true if the query desires 'Video' results back.
     */
    public boolean desiresVideo() {
        return hasMetaMask(VIDEO_MASK);
    }
    
    /** @return true if the query desires 'Document' results back.
     */
    public boolean desiresDocuments() {
        return hasMetaMask(DOC_MASK);
    }
    
    /** @return true if the query desires 'Image' results back.
     */
    public boolean desiresImages() {
        return hasMetaMask(IMAGE_MASK);
    }
    
    /** @return true if the query desires 'Programs/Packages' for Windows
     *  results back.
     */
    public boolean desiresWindowsPrograms() {
        return hasMetaMask(WIN_PROG_MASK);  
    }
    
    /** @return true if the query desires 'Programs/Packages' for Linux/OSX
     *  results back.
     */
    public boolean desiresLinuxOSXPrograms() {
        return hasMetaMask(LIN_PROG_MASK);
    }
    
    @Override
    public boolean desiresTorrents() {
        return hasMetaMask(TORRENT_MASK);
    }
    
    private boolean hasMetaMask(int mask) {
        if (_metaMask != null) 
            return ((_metaMask & mask) > 0);
        return true;
    }
    
    /**
     * Returns the mask of allowed programs.
     */
    public int getMetaMask() {
        if (_metaMask != null)
            return _metaMask;
        return 0;
    }
    
    /** Marks this as being an re-originated query. */
    public void originate() {
        originated = true;
    }
    
    /** Determines if this is an originated query. */
    public boolean isOriginated() {
        return originated;
    }
    
    public boolean shouldIncludeXMLInResponse() {
        return desiresXMLResponses() || desiresOutOfBandReplies();
    }
    
    @Override
    public Class<? extends Message> getHandlerClass() {
        return QueryRequest.class;
    }

    /**
     * @effects Writes given extension string to given stream, adding
     * delimiter if necessary, reporting whether next call should add
     * delimiter. ext may be null or zero-length, in which case this is noop.
     */
    protected boolean writeGemExtension(OutputStream os, 
                                        boolean addPrefixDelimiter, 
                                        byte[] extBytes) throws IOException {
        if (extBytes == null || (extBytes.length == 0)) {
            return addPrefixDelimiter;
        }
        if(addPrefixDelimiter) {
            os.write(0x1c);
        }
        os.write(extBytes);
        return true; // any subsequent extensions should have delimiter 
    }
    
     /**
     * @effects Writes each extension string in exts to given stream,
     * adding delimiters as necessary. exts may be null or empty, in
     *  which case this is noop.
     */
    protected boolean writeGemExtensions(OutputStream os, 
                                         boolean addPrefixDelimiter, 
                                         Iterator<URN> iter) throws IOException {
        while(iter.hasNext()) {
            addPrefixDelimiter = writeGemExtension(os, addPrefixDelimiter, 
                    StringUtils.toAsciiBytes(iter.next().toString()));
        }
        return addPrefixDelimiter; // will be true is anything at all was written 
    }
    
    /**
     * @effects utility function to read null-terminated byte[] from stream
     */
    protected static byte[] readNullTerminatedBytes(InputStream is) 
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        while ((is.available()>0)&&(i=is.read())!=0) {
            baos.write(i);
        }
        return baos.toByteArray();
    }

    @Override
    public int hashCode() {
        if (_hashCode == 0) {
            int result = 17;
            result = (37 * result) + QUERY.hashCode();
            if (XML_DOC != null)
                result = (37 * result) + XML_DOC.hashCode();
            result = (37 * result) + QUERY_URNS.hashCode();
            if (QUERY_KEY != null) {
                result = (37 * result) + QUERY_KEY.hashCode();
            }
            // TODO:: ADD GUID!!
            _hashCode = result;
        }
        return _hashCode;
    }

    // overrides Object.toString
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof QueryRequestImpl))
            return false;
        QueryRequestImpl qr = (QueryRequestImpl) o;
        return (MIN_SPEED == qr.MIN_SPEED && QUERY.equals(qr.QUERY)
                && (XML_DOC == null ? qr.XML_DOC == null : XML_DOC.equals(qr.XML_DOC))
                && QUERY_URNS.equals(qr.QUERY_URNS) && Arrays.equals(getGUID(), qr.getGUID()) && Arrays
                .equals(PAYLOAD, qr.PAYLOAD));
    }

    @Override
    public String toString() {
        return "<query: \"" + getQuery() + "\", " + "ttl: " + getTTL() + ", " + "hops: "
                + getHops() + ", " + "meta: \"" + getRichQueryString() + "\", " + "urns: "
                + getQueryUrns().size() + ">";
    }

    static byte[] patchInGGEP(byte[] payload, GGEP ggep, MACCalculatorRepositoryManager manager)
            throws BadPacketException {
        QueryRequestPayloadParser parser = new QueryRequestPayloadParser(payload, manager);
        HUGEExtension huge = parser.huge;
        if (huge != null) {
            // we write in the last modifiable block if available, so our
            // values are still there in the merged version that is read back
            // from the network: this is not good
            GGEPBlock block = getLastBlock(huge.getGGEPBlocks());
            if (block != null) {
                GGEP merge = new GGEP(true);
                // first merge in original block and then ours, to make sure
                // our values prevail
                merge.merge(block.getGGEP());
                merge.merge(ggep);
                return insertBytes(payload, parser.hugeStart + block.getStartPos(), parser.hugeStart + block.getEndPos(), merge.toByteArray());
            }
        }
        if (isFirstNullByteAfterOffset(payload, payload.length - 1, 2)) {
            // if ggep is appended after query string keep 0 delimiter
            return insertGGEP(payload, payload.length, payload.length, ggep.toByteArray(), true);
        }
        else if (payload[payload.length - 1] != 0x1C) {
            return insertGGEP(payload, payload.length - 1, payload.length - 1, ggep.toByteArray(), true);   
        }
        else {
            return insertGGEP(payload, payload.length, payload.length, ggep.toByteArray(), false);
        }
    }
    
    /**
     * Returns true if byte at <code>index</code> is 0 and it's the first
     * one in the payload signifying the end of the query string.
     */
    private static boolean isFirstNullByteAfterOffset(byte[] payload, int index, int offset) {
        if (payload[index] != 0x00) {
            return false;
        }
        for (int i = offset; i < index; i++) {
            if (payload[i] == 0x00) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Return the last GGEPBlock in the list or null if there
     * is none or if the list is empty.
     */
    private static GGEPBlock getLastBlock(List<GGEPBlock> blocks) {
        return blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
    }
    
    private static byte[] insertGGEP(byte[] payload, int start, int end, byte[] ggepBytes, boolean prependDelimiter) {
        if (prependDelimiter) {
            byte[] ggepBlock = new byte[ggepBytes.length  +  1];
            // set HUGE delimiter
            ggepBlock[0] = 0x1C;
            System.arraycopy(ggepBytes, 0, ggepBlock, 1, ggepBytes.length);
            return insertBytes(payload, start, end, ggepBlock);
        }
        else {
            return insertBytes(payload, start, end, ggepBytes);
        }
    }
    
    private static byte[] insertBytes(byte[] payload, int start, int end, byte[] ggepBytes) {
        byte[] newPayload = new byte[payload.length + ggepBytes.length - (end - start)];
        
        System.arraycopy(payload, 0, newPayload, 0, start);
        System.arraycopy(ggepBytes, 0, newPayload, start, ggepBytes.length);
        
        if (end < payload.length) {
            System.arraycopy(payload, end, newPayload, start + ggepBytes.length, payload.length - end);
        }
        
        return newPayload;
    }
    
    static class QueryRequestPayloadParser {
        
        String query = "";
        String richQuery = "";
        int minSpeed = 0;
        Set<URN> queryUrns = null;
        Set<URN.Type> requestedUrnTypes = null;
        AddressSecurityToken addressSecurityToken = null;
        
        HUGEExtension huge;
        
        int featureSelector;
        
        boolean doNotProxy;
        
        boolean doNotProxyV3;
        
        Integer metaMask;
        
        boolean hasSecurityTokenRequest;
        
        boolean partialResultsDesired;
        
        boolean desiresNMS1Urns;
        
        int hugeStart;
        
        int hugeEnd;
        
        public QueryRequestPayloadParser(byte[] payload, MACCalculatorRepositoryManager manager) throws BadPacketException {
            try {
                PositionByteArrayInputStream bais = new PositionByteArrayInputStream(payload);
                short sp = ByteUtils.leb2short(bais);
                minSpeed = ByteUtils.ushort2int(sp);
                query = new String(readNullTerminatedBytes(bais), "UTF-8");
                 
                // handle extensions, which include rich query and URN stuff
                hugeStart = bais.getPos();
                byte[] extsBytes = readNullTerminatedBytes(bais);
                huge = new HUGEExtension(extsBytes);
                hugeEnd = bais.getPos();
                GGEP ggep = huge.getGGEP();

                if(ggep != null) {
                    try {
                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                            byte[] qkBytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT);
                            addressSecurityToken = new AddressSecurityToken(qkBytes, manager);
                        }
                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_FEATURE_QUERY))
                            featureSelector = ggep.getInt(GGEPKeys.GGEP_HEADER_FEATURE_QUERY);
                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_NO_PROXY)) {
                            doNotProxy = true;
                        }
                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_META)) {
                            metaMask = ggep.getInt(GGEPKeys.GGEP_HEADER_META);
                            // if the value is something we can't handle, don't even set it
                            if ((metaMask < 4) || (metaMask > 248))
                                metaMask = null;
                        }
                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_OOB)) {
                            hasSecurityTokenRequest = true;
                        }
                        
                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_PARTIAL_RESULT_PREFIX))
                            partialResultsDesired = true;

                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_NMS1)) {
                            desiresNMS1Urns = true;
                        }
                        
                        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_EXTENDED_QUERY)) {
                            this.query = ggep.getString(GGEPKeys.GGEP_HEADER_EXTENDED_QUERY);
                        }
                        
                    } catch (BadGGEPPropertyException ignored) {}
                }

                queryUrns = huge.getURNS();
                requestedUrnTypes = huge.getURNTypes();
                for(String currMiscBlock : huge.getMiscBlocks()) {
                    if(!richQuery.equals(""))
                        break;
                    if (currMiscBlock.startsWith("<?xml"))
                        richQuery = currMiscBlock;                
                }
            } catch(UnsupportedEncodingException uee) {
                //couldn't build query from network due to unsupportedencodingexception
                //so throw a BadPacketException 
                throw new BadPacketException(uee.getMessage());
            } catch (IOException ioe) {
                ErrorService.error(ioe);
            }
        }
        
        static class PositionByteArrayInputStream extends ByteArrayInputStream {

            public PositionByteArrayInputStream(byte[] buf) {
                super(buf);
            }
            
            public int getPos() {
                return pos;
            }
            
        }
        
    }
    
}
