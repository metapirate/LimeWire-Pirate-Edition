package com.limegroup.gnutella.spam;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.util.Base32;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseVerifier;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.XMLStringUtils;

/**
 * This class splits a RemoteFileDesc or a QueryRequest into tokens that will be
 * put into the RatingTable.
 * <p>
 * Currently, it extracts the following data to build a token:
 * <ul>
 * <li>keywords from the file name or query string</li>
 * <li>name/value pairs from the XML metadata (if any)</li>
 * <li>file urn (if any)</li>
 * <li>file size</li>
 * <li>address (but not port) of the sender</li>
 * </ul>
 * 
 * The vendor string is no longer used, since it's too easy for spammers to
 * forge.
 */
@Singleton
public class Tokenizer {
    private static final Log LOG = LogFactory.getLog(Tokenizer.class);

    /**
     * The maximum length of a keyword in chars; keywords longer than this will
     * be truncated. We use chars rather than bytes to avoid corrupting
     * multi-byte chars when truncating
     */
    private int MAX_KEYWORD_LENGTH = 8;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final ResponseVerifier responseVerifier;
    private final TemplateHashTokenFactory templateHashTokenFactory;

    @Inject
    Tokenizer(NetworkInstanceUtils networkInstanceUtils,
            ResponseVerifier responseVerifier,
            TemplateHashTokenFactory templateHashTokenFactory) {
        this.networkInstanceUtils = networkInstanceUtils;
        this.responseVerifier = responseVerifier;
        this.templateHashTokenFactory = templateHashTokenFactory;
    }

    /**
     * Extracts a set of tokens from a RemoteFileDesc.
     * 
     * @param desc the RemoteFileDesc that should be tokenized
     * @return a non-empty set of Tokens
     */
    public Set<Token> getTokens(RemoteFileDesc desc) {
        Set<Token> set = new HashSet<Token>();
        tokenize(desc, set);
        return set;
    }

    /**
     * Extracts a set of tokens from an array of RemoteFileDescs - useful if the
     * user wants to mark multiple RFDs from a TableLine as spam (or not), which
     * should rate each token only once.
     * 
     * @param descs the array of RemoteFileDescs that should be tokenized
     * @return a non-empty set of Tokens
     */
    public Set<Token> getTokens(RemoteFileDesc[] descs) {
        Set<Token> set = new HashSet<Token>();
        for(RemoteFileDesc desc : descs)
            tokenize(desc, set);
        return set;
    }

    /**
     * Extracts a set of tokens from a RemoteFileDesc.
     * 
     * @param desc the RemoteFileDesc that should be tokenized
     * @param set the set to which the tokens should be added
     */
    private void tokenize(RemoteFileDesc desc, Set<Token> set) {
        if(LOG.isDebugEnabled()) {
            String addr = desc.getAddress().getAddressDescription();
            LOG.debug("Tokenizing result from " + addr);
        }
        String name = desc.getFileName();
        byte[] queryGUID = desc.getQueryGUID();
        if(queryGUID != null) {
            String query = responseVerifier.getQueryString(queryGUID);
            if(query != null) {
                Token t = templateHashTokenFactory.create(query, name);
                if(t != null)
                    set.add(t);
            }
        }
        getKeywordTokens(FileUtils.getFilenameNoExtension(name), set);
        String ext = FileUtils.getFileExtension(name);
        if(!ext.equals(""))
            set.add(new FileExtensionToken(ext));
        LimeXMLDocument doc = desc.getXMLDocument();
        if(doc != null) {
            getKeywordTokens(doc, set);
            String infohash = doc.getValue(LimeXMLNames.TORRENT_INFO_HASH);
            if(infohash != null)
                set.add(new UrnToken("urn:sha1:" + infohash));
        }
        URN urn = desc.getSHA1Urn();
        if(urn != null)
            set.add(new UrnToken(urn.toString()));
        set.add(new SizeToken(desc.getSize()));
        set.add(new ApproximateSizeToken(desc.getSize()));
        // Ignore friend addresses and private addresses such as 192.168.x.x
        Address address = desc.getAddress();
        if(address instanceof Connectable) {
            Connectable connectable = (Connectable)address;
            if(!networkInstanceUtils.isPrivateAddress(connectable.getInetAddress()))
                set.add(new AddressToken(connectable.getAddress()));
        }
        set.add(new ClientGUIDToken(Base32.encode(desc.getClientGUID())));
    }

    /**
     * Tokenizes a QueryReply. Keywords from the filenames and XML metadata are
     * ignored, but templates are extracted from the filenames.
     * 
     * @param qr the QueryReply that should be tokenized
     * @return a non-empty set of Tokens
     */
    public Set<Token> getNonKeywordTokens(QueryReply qr) {
        if(LOG.isDebugEnabled())
            LOG.debug("Tokenizing query reply from " + qr.getIP());
        Set<Token> set = new HashSet<Token>();
        String query = responseVerifier.getQueryString(qr.getGUID());
        // Client GUID
        set.add(new ClientGUIDToken(Base32.encode(qr.getClientGUID())));
        // Responder's address, unless private
        String ip = qr.getIP();
        if(!networkInstanceUtils.isPrivateAddress(ip))
            set.add(new AddressToken(ip));
        try {
            for(Response r : qr.getResultsArray()) {
                // Template
                if(query != null) {
                    Token t = templateHashTokenFactory.create(query, r.getName());
                    if(t != null)
                        set.add(t);
                }
                // URNs
                for(URN urn : r.getUrns())
                    set.add(new UrnToken(urn.toString()));
                LimeXMLDocument doc = r.getDocument();
                if(doc != null) {
                    String infohash = doc.getValue(LimeXMLNames.TORRENT_INFO_HASH);
                    if(infohash != null)
                        set.add(new UrnToken("urn:sha1:" + infohash));
                }
                // File sizes
                long size = r.getSize();
                set.add(new SizeToken(size));
                set.add(new ApproximateSizeToken(size));
                // Alt-loc addresses, unless private
                for(IpPort ipp : r.getLocations()) {
                    ip = ipp.getInetAddress().getHostAddress();
                    if(!networkInstanceUtils.isPrivateAddress(ip))
                        set.add(new AddressToken(ip));
                }
            }
        } catch(BadPacketException ignored) {}
        return set;
    }

    /**
     * Tokenizes a QueryRequest, including the search terms, XML metadata and
     * URN (if any) - we clear the spam ratings of search tokens and ignore them
     * for spam rating purposes for the rest of the session.
     * 
     * @param qr the QueryRequest that should be tokenized
     * @return a set of Tokens, may be empty
     */
    public Set<Token> getTokens(QueryRequest qr) {
        if(LOG.isDebugEnabled())
            LOG.debug("Tokenizing " + qr);
        Set<Token> set = new HashSet<Token>();
        getKeywordTokens(qr.getQuery(), set);
        LimeXMLDocument xml = qr.getRichQuery();
        if(xml != null)
            getKeywordTokens(xml, set);
        Set<URN> urns = qr.getQueryUrns();
        for(URN urn : urns)
            set.add(new UrnToken(urn.toString()));
        return set;
    }

    /**
     * Extracts KeywordTokens from an XML metadata document.
     * 
     * @param doc the LimeXMLDocument that should be tokenized
     * @param set the set to which the tokens should be added
     */
    private void getKeywordTokens(LimeXMLDocument doc, Set<Token> set) {
        for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
            String name = entry.getKey().toString();
            String value = entry.getValue().toString();
            getXMLKeywords(name, value, set);
        }
    }

    /**
     * Extracts XMLKeywordTokens from the field name and value of an XML
     * metadata item.
     * 
     * @param name the field name as a String (eg audios_audio_bitrate)
     * @param value the value as a String
     * @param set the set to which the tokens should be added
     */
    private void getXMLKeywords(String name, String value, Set<Token> set) {
        name = extractSimpleFieldName(name);
        name.toLowerCase(Locale.US);
        value.toLowerCase(Locale.US);
        for(String keyword : QueryUtils.extractKeywords(value, false)) {
            if(keyword.length() > MAX_KEYWORD_LENGTH)
                keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
            set.add(new XMLKeywordToken(name, keyword));
        }
    }

    /**
     * Extracts the last part of the field name for a canonical field name (eg
     * audios_audio_bitrate becomes bitrate).
     * 
     * @param name the canonical field name
     * @return the last part of the canonical field name
     */
    private String extractSimpleFieldName(String name) {
        int idx1 = name.lastIndexOf(XMLStringUtils.DELIMITER);
        int idx2 = name.lastIndexOf(XMLStringUtils.DELIMITER, idx1 - 1);
        return name.substring(idx2 + XMLStringUtils.DELIMITER.length(), idx1);
    }

    /**
     * Splits a String into keyword tokens using QueryUtils.extractKeywords().
     * 
     * @param str the String to tokenize
     * @param set the set to which the tokens should be added
     */
    private void getKeywordTokens(String str, Set<Token> set) {
        str.toLowerCase(Locale.US);
        for(String keyword : QueryUtils.extractKeywords(str, false)) {
            if(keyword.length() > MAX_KEYWORD_LENGTH)
                keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
            set.add(new KeywordToken(keyword));
        }
    }
}