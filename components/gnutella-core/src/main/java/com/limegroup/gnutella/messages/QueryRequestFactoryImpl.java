package com.limegroup.gnutella.messages;

import java.util.Locale;
import java.util.Set;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

@Singleton
public class QueryRequestFactoryImpl implements QueryRequestFactory {

    private final NetworkManager networkManager;
    private final LimeXMLDocumentFactory limeXMLDocumentFactory;
    private final MACCalculatorRepositoryManager MACCalculatorRepositoryManager;

    @Inject
    public QueryRequestFactoryImpl(NetworkManager networkManager, 
            LimeXMLDocumentFactory limeXMLDocumentFactory,
            MACCalculatorRepositoryManager MACCalculatorRepositoryManager) {
        this.networkManager = networkManager;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.MACCalculatorRepositoryManager = MACCalculatorRepositoryManager;
        
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createRequery(com.limegroup.gnutella.URN)
     */
    public QueryRequest createRequery(URN sha1) {
        if (sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        Set<URN> sha1Set = new UrnSet(sha1);
        return createQueryRequest(QueryRequestImpl.newQueryGUID(true),
                QueryRequest.DEFAULT_TTL, QueryRequest.DEFAULT_URN_QUERY, "",
                sha1Set, null, !networkManager.acceptedIncomingConnection(),
                Network.UNKNOWN, false, 0, false, 0);

    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(com.limegroup.gnutella.URN)
     */
    public QueryRequest createQuery(URN sha1) {
        if (sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        Set<URN> sha1Set = new UrnSet(sha1);
        return createQueryRequest(QueryRequestImpl.newQueryGUID(false),
                QueryRequest.DEFAULT_TTL, QueryRequest.DEFAULT_URN_QUERY, "",
                sha1Set, null, !networkManager.acceptedIncomingConnection(),
                Network.UNKNOWN, false, 0, false, 0);

    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(com.limegroup.gnutella.URN, java.lang.String)
     */
    public QueryRequest createQuery(URN sha1, String filename) {
        if (sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        if (filename == null) {
            throw new NullPointerException("null query");
        }
        if (filename.length() == 0) {
            filename = QueryRequest.DEFAULT_URN_QUERY;
        }
        Set<URN> sha1Set = new UrnSet(sha1);
        return createQueryRequest(QueryRequestImpl.newQueryGUID(false),
                QueryRequest.DEFAULT_TTL, filename, "", sha1Set, null,
                !networkManager.acceptedIncomingConnection(), Network.UNKNOWN,
                false, 0, false, 0);

    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createRequery(com.limegroup.gnutella.URN, byte)
     */
    public QueryRequest createRequery(URN sha1, byte ttl) {
        if (sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        if (ttl <= 0 || ttl > 6) {
            throw new IllegalArgumentException("invalid TTL: " + ttl);
        }
        Set<URN> sha1Set = new UrnSet(sha1);
        return createQueryRequest(QueryRequestImpl.newQueryGUID(true), ttl,
                QueryRequest.DEFAULT_URN_QUERY, "", sha1Set, null,
                !networkManager.acceptedIncomingConnection(), Network.UNKNOWN,
                false, 0, false, 0);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(java.util.Set)
     */
    public QueryRequest createQuery(Set<? extends URN> urnSet) {
        if (urnSet == null)
            throw new NullPointerException("null urnSet");
        return createQueryRequest(QueryRequestImpl.newQueryGUID(false),
                QueryRequest.DEFAULT_TTL, QueryRequest.DEFAULT_URN_QUERY, "",
                urnSet, null, !networkManager.acceptedIncomingConnection(),
                Network.UNKNOWN, false, 0, false, 0);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createRequery(java.lang.String)
     */
    public QueryRequest createRequery(String query) {
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (query.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        return create(QueryRequestImpl.newQueryGUID(true), query);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(java.lang.String)
     */
    public QueryRequest createQuery(String query) {
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (query.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        return create(QueryRequestImpl.newQueryGUID(false), query);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createOutOfBandQuery(byte[], java.lang.String, java.lang.String)
     */
    public QueryRequest createOutOfBandQuery(byte[] guid, String query,
            String xmlQuery) {
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (xmlQuery == null) {
            throw new NullPointerException("null xml query");
        }
        if (query.length() == 0 && xmlQuery.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
            throw new IllegalArgumentException("invalid XML");
        }
        return create(guid, QueryRequest.DEFAULT_TTL, query, xmlQuery, true);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createOutOfBandQuery(byte[], java.lang.String, java.lang.String, com.limegroup.gnutella.MediaType)
     */
    public QueryRequest createOutOfBandQuery(byte[] guid, String query,
            String xmlQuery, SearchCategory type) {
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (xmlQuery == null) {
            throw new NullPointerException("null xml query");
        }
        if (query.length() == 0 && xmlQuery.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
            throw new IllegalArgumentException("invalid XML");
        }
        return create(guid, QueryRequest.DEFAULT_TTL, query, xmlQuery, true,
                type);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createOutOfBandQuery(java.lang.String, byte[], int)
     */
    public QueryRequest createOutOfBandQuery(String query, byte[] ip, int port) {
        byte[] guid = GUID.makeAddressEncodedGuid(ip, port);
        return createOutOfBandQuery(guid, query, "");
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createWhatIsNewQuery(byte[], byte)
     */
    public QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl) {
        return createWhatIsNewQuery(guid, ttl, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createWhatIsNewQuery(byte[], byte, com.limegroup.gnutella.MediaType)
     */
    public QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl,
            SearchCategory type) {
        if (ttl < 1)
            throw new IllegalArgumentException("Bad TTL.");
        return createQueryRequest(guid, ttl,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null,
                !networkManager.acceptedIncomingConnection(), Network.UNKNOWN,
                false, FeatureSearchData.WHAT_IS_NEW, false, getMetaFlag(type));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createWhatIsNewOOBQuery(byte[], byte)
     */
    public QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl) {
        return createWhatIsNewOOBQuery(guid, ttl, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createWhatIsNewOOBQuery(byte[], byte, com.limegroup.gnutella.MediaType)
     */
    public QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl,
            SearchCategory type) {
        if (ttl < 1)
            throw new IllegalArgumentException("Bad TTL.");
        return createQueryRequest(guid, ttl,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null,
                !networkManager.acceptedIncomingConnection(), Network.UNKNOWN,
                true, FeatureSearchData.WHAT_IS_NEW, false, getMetaFlag(type));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(java.lang.String, java.lang.String)
     */
    public QueryRequest createQuery(String query, String xmlQuery) {
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (xmlQuery == null) {
            throw new NullPointerException("null xml query");
        }
        if (query.length() == 0 && xmlQuery.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
            throw new IllegalArgumentException("invalid XML");
        }
        return create(QueryRequestImpl.newQueryGUID(false), query, xmlQuery);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(java.lang.String, byte)
     */
    public QueryRequest createQuery(String query, byte ttl) {
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (query.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        if (ttl <= 0 || ttl > 6) {
            throw new IllegalArgumentException("invalid TTL: " + ttl);
        }
        return create(QueryRequestImpl.newQueryGUID(false), ttl, query);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(byte[], java.lang.String, java.lang.String)
     */
    public QueryRequest createQuery(byte[] guid, String query, String xmlQuery) {
        return createQuery(guid, query, xmlQuery, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(byte[], java.lang.String, java.lang.String, com.limegroup.gnutella.MediaType)
     */
    public QueryRequest createQuery(byte[] guid, String query, String xmlQuery,
            SearchCategory type) {
        if (guid == null) {
            throw new NullPointerException("null guid");
        }
        if (guid.length != 16) {
            throw new IllegalArgumentException("invalid guid length");
        }
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (xmlQuery == null) {
            throw new NullPointerException("null xml query");
        }
        if (query.length() == 0 && xmlQuery.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        if (xmlQuery.length() != 0 && !xmlQuery.startsWith("<?xml")) {
            throw new IllegalArgumentException("invalid XML");
        }
        return create(guid, QueryRequest.DEFAULT_TTL, query, xmlQuery, type);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createProxyQuery(com.limegroup.gnutella.messages.QueryRequest, byte[])
     */
    public QueryRequest createProxyQuery(QueryRequest qr, byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentException("bad guid size: " + guid.length);

        // i can't just call a new constructor, since there might be stuff in
        // the payload we don't understand and would get lost
        byte[] payload = qr.getPayload();
        byte[] newPayload = new byte[payload.length];
        System.arraycopy(payload, 0, newPayload, 0, newPayload.length);
        // disable old out of band if requested
        if (SearchSettings.DISABLE_OOB_V2.getBoolean())
            newPayload[0] &= ~QueryRequest.SPECIAL_OUTOFBAND_MASK;
        else
            newPayload[0] |= QueryRequest.SPECIAL_OUTOFBAND_MASK;
        GGEP ggep = new GGEP(true);
        // signal oob capability
        ggep.put(GGEPKeys.GGEP_HEADER_SECURE_OOB);

        try {
            newPayload = QueryRequestImpl.patchInGGEP(newPayload, ggep, MACCalculatorRepositoryManager);
            return createNetworkQuery(guid, qr.getTTL(), qr.getHops(),
                    newPayload, qr.getNetwork());
        } catch (BadPacketException ioe) {
            throw new IllegalArgumentException(ioe.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createDoNotProxyQuery(com.limegroup.gnutella.messages.QueryRequest)
     */
    public QueryRequest createDoNotProxyQuery(QueryRequest qr) {
        if (!GUID.isLimeGUID(qr.getGUID())) {
            throw new IllegalArgumentException(
                    "query request from different vendor cannot not be unmarked");
        }
        if (!qr.isOriginated()) {
            throw new IllegalArgumentException("query not originated from here");
        }

        // only used for queries understood by us
        // so we can use the copy constructor and set OOB to false
        return createQueryRequest(qr.getGUID(), qr.getTTL(), qr.getMinSpeed(),
                qr.getQuery(), qr.getRichQueryString(), qr.getQueryUrns(), qr
                        .getQueryKey(), qr.isFirewalledSource(), qr
                        .getNetwork(), qr.desiresOutOfBandReplies(), qr
                        .getFeatureSelector(), true, qr.getMetaMask(), false); // no
        // normalization
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQuery(com.limegroup.gnutella.messages.QueryRequest, byte)
     */
    public QueryRequest createQuery(QueryRequest qr, byte ttl) {
        // Construct a query request that is EXACTLY like the other query,
        // but with a different TTL.
        try {
            return createNetworkQuery(qr.getGUID(), ttl, qr.getHops(), qr
                    .getPayload(), qr.getNetwork());
        } catch (BadPacketException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#unmarkOOBQuery(com.limegroup.gnutella.messages.QueryRequest)
     */
    public QueryRequest unmarkOOBQuery(QueryRequest qr) {
        if (!GUID.isLimeGUID(qr.getGUID())) {
            throw new IllegalArgumentException(
                    "query request from different vendor cannot not be unmarked");
        }

        // only used for queries understood by us
        // so we can use the copy constructor and set OOB to false
        return createQueryRequest(qr.getGUID(), qr.getTTL(), qr.getQuery(), qr
                .getRichQueryString(), qr.getQueryUrns(), qr.getQueryKey(), qr
                .isFirewalledSource(), qr.getNetwork(), false, qr
                .getFeatureSelector(), qr.doNotProxy(), qr.getMetaMask());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQueryKeyQuery(java.lang.String, org.limewire.security.AddressSecurityToken)
     */
    public QueryRequest createQueryKeyQuery(String query,
            AddressSecurityToken key) {
        if (query == null) {
            throw new NullPointerException("null query");
        }
        if (query.length() == 0) {
            throw new IllegalArgumentException("empty query");
        }
        if (key == null) {
            throw new NullPointerException("null query key");
        }
        return createQueryRequest(QueryRequestImpl.newQueryGUID(false), (byte) 1,
                query, "", URN.NO_URN_SET, key, !networkManager
                        .acceptedIncomingConnection(), Network.UNKNOWN, false,
                0, false, 0);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQueryKeyQuery(com.limegroup.gnutella.URN, org.limewire.security.AddressSecurityToken)
     */
    public QueryRequest createQueryKeyQuery(URN sha1, AddressSecurityToken key) {
        if (sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        if (key == null) {
            throw new NullPointerException("null query key");
        }
        Set<URN> sha1Set = new UrnSet(sha1);
        return createQueryRequest(QueryRequestImpl.newQueryGUID(false), (byte) 1,
                QueryRequest.DEFAULT_URN_QUERY, "", sha1Set, key,
                !networkManager.acceptedIncomingConnection(), Network.UNKNOWN,
                false, 0, false, 0);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createMulticastQuery(byte[], com.limegroup.gnutella.messages.QueryRequest)
     */
    public QueryRequest createMulticastQuery(byte[] guid, QueryRequest qr) {
        if (qr == null)
            throw new NullPointerException("null query");

        // modify the payload to not be OOB.
        byte[] payload = qr.getPayload();
        byte[] newPayload = new byte[payload.length];
        System.arraycopy(payload, 0, newPayload, 0, newPayload.length);
        newPayload[0] &= ~QueryRequest.SPECIAL_OUTOFBAND_MASK;
        newPayload[0] |= QueryRequest.SPECIAL_XML_MASK;

        try {
            return createNetworkQuery(guid, (byte) 1, qr.getHops(), newPayload,
                    Network.MULTICAST);
        } catch (BadPacketException ioe) {
            throw new IllegalArgumentException(ioe.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQueryKeyQuery(com.limegroup.gnutella.messages.QueryRequest, org.limewire.security.AddressSecurityToken)
     */
    public QueryRequest createQueryKeyQuery(QueryRequest qr,
            AddressSecurityToken key) {

        // TODO: Copy the payload verbatim, except add the query-key
        // into the GGEP section.
        return createQueryRequest(qr.getGUID(), qr.getTTL(), qr.getQuery(), qr
                .getRichQueryString(), qr.getQueryUrns(), key, qr
                .isFirewalledSource(), Network.UNKNOWN, qr
                .desiresOutOfBandReplies(), qr.getFeatureSelector(), false, qr
                .getMetaMask());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createNonFirewalledQuery(java.lang.String, byte)
     */
    public QueryRequest createNonFirewalledQuery(String query, byte ttl) {
        return createQueryRequest(QueryRequestImpl.newQueryGUID(false), ttl, query,
                "", URN.NO_URN_SET, null, false, Network.UNKNOWN, false, 0,
                false, 0);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createNetworkQuery(byte[], byte, byte, byte[], com.limegroup.gnutella.messages.Message.Network)
     */
    public QueryRequest createNetworkQuery(byte[] guid, byte ttl, byte hops,
            byte[] payload, Network network) throws BadPacketException {
        return new QueryRequestImpl(guid, ttl, hops, payload, network, limeXMLDocumentFactory, MACCalculatorRepositoryManager);
    }

    /**
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid. GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest create(byte[] guid, String query) {
        return create(guid, query, "");
    }

    /**
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid. GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest create(byte[] guid, byte ttl, String query) {
        return create(guid, ttl, query, "");
    }

    /**
     * Builds a new query from scratch, with no metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid. GUID must have
     * been created via newQueryGUID; this allows the caller to match up results
     */
    private QueryRequest create(byte[] guid, String query, String xmlQuery) {
        return create(guid, QueryRequest.DEFAULT_TTL, query, xmlQuery);
    }

    /**
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid. GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    private QueryRequest create(byte[] guid, byte ttl, String query,
            String richQuery) {
        return createQueryRequest(guid, ttl, query, richQuery, URN.NO_URN_SET,
                null, !networkManager.acceptedIncomingConnection(),
                Network.UNKNOWN, false, 0, false, 0);
    }

    /**
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid. GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    private QueryRequest create(byte[] guid, byte ttl, String query,
            String richQuery, SearchCategory type) {
        return createQueryRequest(guid, ttl, getQuery(query, type), richQuery, URN.NO_URN_SET,
                null, !networkManager.acceptedIncomingConnection(),
                Network.UNKNOWN, false, 0, false, getMetaFlag(type));
    }

    /**
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid. GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    private QueryRequest create(byte[] guid, byte ttl, String query,
            String richQuery, boolean canReceiveOutOfBandReplies) {
        return createQueryRequest(guid, ttl, query, richQuery, URN.NO_URN_SET,
                null, !networkManager.acceptedIncomingConnection(),
                Network.UNKNOWN, canReceiveOutOfBandReplies, 0, false, 0);
    }

    /**
     * Builds a new query from scratch, with metadata, using the given GUID.
     * Whether or not this is a repeat query is encoded in guid. GUID must have
     * been created via newQueryGUID; this allows the caller to match up
     * results.
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     */
    private QueryRequest create(byte[] guid, byte ttl, String query,
            String richQuery, boolean canReceiveOutOfBandReplies, SearchCategory type) {
        return createQueryRequest(guid, ttl, getQuery(query, type), richQuery, URN.NO_URN_SET,
                null, !networkManager.acceptedIncomingConnection(),
                Network.UNKNOWN, canReceiveOutOfBandReplies, 0, false,
                getMetaFlag(type));
    }
    
    private String getQuery(String query, SearchCategory category) {
        if (category == SearchCategory.TORRENT && SearchSettings.APPEND_TORRENT_TO_TORRENT_QUERIES.getValue()
                && query.toLowerCase(Locale.US).indexOf("torrent")  < 0) {
            return query + " torrent";
        }
        return query;
    }

    private int getMetaFlag(SearchCategory type) {
        int metaFlag = 0;
        if (type == null)
            ;
        else if (type == SearchCategory.AUDIO)
            metaFlag |= QueryRequest.AUDIO_MASK;
        else if (type == SearchCategory.VIDEO)
            metaFlag |= QueryRequest.VIDEO_MASK;
        else if (type == SearchCategory.IMAGE)
            metaFlag |= QueryRequest.IMAGE_MASK;
        else if (type == SearchCategory.DOCUMENT)
            metaFlag |= QueryRequest.DOC_MASK;
        else if (type == SearchCategory.TORRENT)
            // Always append the mask since no recognised mask falls over to ALL anyways
            metaFlag |= QueryRequest.TORRENT_MASK;
        else if (type == SearchCategory.PROGRAM) {
            if (OSUtils.isLinux() || OSUtils.isMacOSX())
                metaFlag |= QueryRequest.LIN_PROG_MASK;
            else if (OSUtils.isWindows())
                metaFlag |= QueryRequest.WIN_PROG_MASK;
            else
                // Other OS, search any type of programs
                metaFlag |= (QueryRequest.LIN_PROG_MASK | QueryRequest.WIN_PROG_MASK);
        }
        return metaFlag;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQueryRequest(byte[], byte, java.lang.String, java.lang.String, java.util.Set, org.limewire.security.AddressSecurityToken, boolean, com.limegroup.gnutella.messages.Message.Network, boolean, int)
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, String query,
            String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector) {
        return createQueryRequest(guid, ttl, query, richQuery, queryUrns,
                addressSecurityToken, isFirewalled, network,
                canReceiveOutOfBandReplies, featureSelector, false, 0);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQueryRequest(byte[], byte, java.lang.String, java.lang.String, java.util.Set, org.limewire.security.AddressSecurityToken, boolean, com.limegroup.gnutella.messages.Message.Network, boolean, int, boolean, int)
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, String query,
            String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector, boolean doNotProxy, int metaFlagMask) {
        return createQueryRequest(guid, ttl, 0, query, richQuery, queryUrns,
                addressSecurityToken, isFirewalled, network,
                canReceiveOutOfBandReplies, featureSelector, doNotProxy,
                metaFlagMask, true);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQueryRequest(byte[], byte, int, java.lang.String, java.lang.String, java.util.Set, org.limewire.security.AddressSecurityToken, boolean, com.limegroup.gnutella.messages.Message.Network, boolean, int, boolean, int)
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, int minSpeed,
            String query, String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector, boolean doNotProxy, int metaFlagMask) {
        return createQueryRequest(guid, ttl, minSpeed, query, richQuery,
                queryUrns, addressSecurityToken, isFirewalled, network,
                canReceiveOutOfBandReplies, featureSelector, doNotProxy,
                metaFlagMask, true);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.QueryRequestFactory#createQueryRequest(byte[], byte, int, java.lang.String, java.lang.String, java.util.Set, org.limewire.security.AddressSecurityToken, boolean, com.limegroup.gnutella.messages.Message.Network, boolean, int, boolean, int, boolean)
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, int minSpeed,
            String query, String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector, boolean doNotProxy, int metaFlagMask,
            boolean normalize) {
        return new QueryRequestImpl(guid, ttl, minSpeed, query, richQuery,
                queryUrns, addressSecurityToken, isFirewalled, network,
                canReceiveOutOfBandReplies, featureSelector, doNotProxy,
                metaFlagMask, normalize, networkManager.canDoFWT(), limeXMLDocumentFactory);
    }

}
