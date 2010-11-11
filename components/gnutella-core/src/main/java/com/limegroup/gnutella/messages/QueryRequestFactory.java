package com.limegroup.gnutella.messages;

import java.util.Set;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.security.AddressSecurityToken;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.Message.Network;

public interface QueryRequestFactory {

    /**
     * Creates a new requery for the specified SHA1 value.
     * 
     * @param sha1 the <tt>URN</tt> of the file to search for
     * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
     * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
     *         is <tt>null</tt>
     */
    public QueryRequest createRequery(URN sha1);

    /**
     * Creates a new query for the specified SHA1 value.
     * 
     * @param sha1 the <tt>URN</tt> of the file to search for
     * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
     * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
     *         is <tt>null</tt>
     */
    public QueryRequest createQuery(URN sha1);

    /**
     * Creates a new query for the specified SHA1 value with file name thrown in
     * for good measure (or at least until \ works as a query).
     * 
     * @param sha1 the <tt>URN</tt> of the file to search for
     * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
     * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
     *         is <tt>null</tt>
     */
    public QueryRequest createQuery(URN sha1, String filename);

    /**
     * Creates a new requery for the specified SHA1 value and the specified
     * firewall boolean.
     * 
     * @param sha1 the <tt>URN</tt> of the file to search for
     * @param ttl the time to live (ttl) of the query
     * @return a new <tt>QueryRequest</tt> for the specified SHA1 value
     * @throws <tt>NullPointerException</tt> if the <tt>sha1</tt> argument
     *         is <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the ttl value is negative
     *         or greater than the maximum allowed value
     */
    public QueryRequest createRequery(URN sha1, byte ttl);

    /**
     * Creates a new query for the specified URN set.
     * 
     * @param urnSet the <tt>Set</tt> of <tt>URNs</tt>s to request.
     * @return a new <tt>QueryRequest</tt> for the specified UrnTypes and URNs
     * @throws <tt>NullPointerException</tt> if either sets are null.
     */
    public QueryRequest createQuery(Set<? extends URN> urnSet);

    /**
     * Creates a requery for when we don't know the hash of the file -- we don't
     * know the hash.
     * 
     * @param query the query string
     * @return a new <tt>QueryRequest</tt> for the specified query
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     */
    public QueryRequest createRequery(String query);

    /**
     * Creates a new query for the specified file name, with no XML.
     * 
     * @param query the file name to search for
     * @return a new <tt>QueryRequest</tt> for the specified query
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     */
    public QueryRequest createQuery(String query);

    /**
     * Creates a new query for the specified file name and the designated XML.
     * 
     * @param guid I trust that this is a address encoded guid. Your loss if it
     *        isn't....
     * @param query the file name to search for
     * @return a new <tt>QueryRequest</tt> for the specified query that has
     *         encoded the input ip and port into the GUID and appropriate
     *         marked the query to signify out of band support.
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     */
    public QueryRequest createOutOfBandQuery(byte[] guid, String query,
            String xmlQuery);

    /**
     * Creates a new query for the specified file name and the designated XML.
     * 
     * @param guid I trust that this is a address encoded guid. Your loss if it
     *        isn't....
     * @param query the file name to search for
     * @param type can be null - the type of results you want.
     * @return a new <tt>QueryRequest</tt> for the specified query that has
     *         encoded the input ip and port into the GUID and appropriate
     *         marked the query to signify out of band support AND specifies a
     *         file type category.
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     */
    public QueryRequest createOutOfBandQuery(byte[] guid, String query,
            String xmlQuery, SearchCategory type);

    /**
     * Creates a new query for the specified file name, with no XML.
     * 
     * @param query the file name to search for
     * @return a new <tt>QueryRequest</tt> for the specified query that has
     *         encoded the input ip and port into the GUID and appropriate
     *         marked the query to signify out of band support.
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     */
    public QueryRequest createOutOfBandQuery(String query, byte[] ip, int port);

    /**
     * Creates a new 'What is new'? query with the specified guid and ttl.
     * 
     * @param guid the desired guid of the query.
     * @param ttl the desired ttl of the query.
     */
    public QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl);

    /**
     * Creates a new 'What is new'? query with the specified guid and ttl.
     * 
     * @param guid the desired guid of the query.
     * @param ttl the desired ttl of the query.
     */
    public QueryRequest createWhatIsNewQuery(byte[] guid, byte ttl,
            SearchCategory type);

    /**
     * Creates a new 'What is new'? OOB query with the specified guid and ttl.
     * 
     * @param guid the desired guid of the query.
     * @param ttl the desired ttl of the query.
     */
    public QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl);

    /**
     * Creates a new 'What is new'? OOB query with the specified guid and ttl.
     * 
     * @param guid the desired guid of the query.
     * @param ttl the desired ttl of the query.
     */
    public QueryRequest createWhatIsNewOOBQuery(byte[] guid, byte ttl,
            SearchCategory type);

    /**
     * Creates a new query for the specified file name, with no XML.
     * 
     * @param query the file name to search for
     * @return a new <tt>QueryRequest</tt> for the specified query
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt> or if the <tt>xmlQuery</tt> argument is
     *         <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument and the xml query are both zero-length (empty)
     */
    public QueryRequest createQuery(String query, String xmlQuery);

    /**
     * Creates a new query for the specified file name, with no XML.
     * 
     * @param query the file name to search for
     * @param ttl the time to live (ttl) of the query
     * @return a new <tt>QueryRequest</tt> for the specified query and ttl
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     * @throws <tt>IllegalArgumentException</tt> if the ttl value is negative
     *         or greater than the maximum allowed value
     */
    public QueryRequest createQuery(String query, byte ttl);

    /**
     * Creates a new query with the specified guid, query string, and xml query
     * string.
     * 
     * @param guid the message GUID for the query
     * @param query the query string
     * @param xmlQuery the xml query string
     * @return a new <tt>QueryRequest</tt> for the specified query, xml query,
     *         and guid
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>, if the <tt>xmlQuery</tt> argument is
     *         <tt>null</tt>, or if the <tt>guid</tt> argument is
     *         <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the guid length is not 16,
     *         if both the query strings are empty, or if the XML does not
     *         appear to be valid
     */
    public QueryRequest createQuery(byte[] guid, String query, String xmlQuery);

    /**
     * Creates a new query with the specified guid, query string, and xml query
     * string.
     * 
     * @param guid the message GUID for the query
     * @param query the query string
     * @param xmlQuery the xml query string
     * @return a new <tt>QueryRequest</tt> for the specified query, xml query,
     *         and guid
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt>, if the <tt>xmlQuery</tt> argument is
     *         <tt>null</tt>, or if the <tt>guid</tt> argument is
     *         <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the guid length is not 16,
     *         if both the query strings are empty, or if the XML does not
     *         appear to be valid
     */
    public QueryRequest createQuery(byte[] guid, String query, String xmlQuery,
            SearchCategory type);

    /**
     * Creates a new OOBquery from the existing query with the specified guid
     * (which should be address encoded).
     * 
     * @param qr the <tt>QueryRequest</tt> to copy
     * @return a new <tt>QueryRequest</tt> with the specified guid that is now
     *         OOB marked.
     * @throws IllegalArgumentException thrown if guid is not right size of if
     *         query is bad.
     */
    public QueryRequest createProxyQuery(QueryRequest qr, byte[] guid);

    /**
     * Copies a query request and marks it to not be proxied.
     * 
     * @throws IllegalArgumentException if the payload is not modifiable
     * @throws IllegalArgumentException if the query request is not from a
     *         LimeWire
     * @throws IllegalArgumentException if {@link #isOriginated()} is false
     */
    public QueryRequest createDoNotProxyQuery(QueryRequest qr);

    /**
     * Creates a new query from the existing query with the specified ttl.
     * 
     * @param qr the <tt>QueryRequest</tt> to copy
     * @param ttl the new ttl
     * @return a new <tt>QueryRequest</tt> with the specified ttl
     */
    public QueryRequest createQuery(QueryRequest qr, byte ttl);

    /**
     * Creates a new query from the existing query and loses the OOB marking.
     * <p>
     * This should only be used for messages that originated from this client.
     * 
     * @param qr the <tt>QueryRequest</tt> to copy
     * @return a new <tt>QueryRequest</tt> with no OOB marking
     * 
     * @throws IllegalArgumentException if the payload is not modifiable
     * @throws IllegalArgumentException if the query request is not from a
     *         LimeWire
     */
    public QueryRequest unmarkOOBQuery(QueryRequest qr);

    /**
     * Creates a new query with the specified query key for use in GUESS-style
     * UDP queries.
     * 
     * @param query the query string
     * @param key the query key
     * @return a new <tt>QueryRequest</tt> instance with the specified query
     *         string and query key
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt> or if the <tt>key</tt> argument is
     *         <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     */
    public QueryRequest createQueryKeyQuery(String query,
            AddressSecurityToken key);

    /**
     * Creates a new query with the specified query key for use in GUESS-style
     * UDP queries.
     * 
     * @param sha1 the URN
     * @param key the query key
     * @return a new <tt>QueryRequest</tt> instance with the specified URN
     *         request and query key
     * @throws <tt>NullPointerException</tt> if the <tt>query</tt> argument
     *         is <tt>null</tt> or if the <tt>key</tt> argument is
     *         <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the <tt>query</tt>
     *         argument is zero-length (empty)
     */
    public QueryRequest createQueryKeyQuery(URN sha1, AddressSecurityToken key);

    /**
     * Creates a new <tt>QueryRequest</tt> instance for multicast queries.
     * This is necessary due to the unique properties of multicast queries, such
     * as the firewalled bit not being set regardless of whether or not the node
     * is truly firewalled/NATted to the world outside the subnet.
     * 
     * @param qr the <tt>QueryRequest</tt> instance containing all the data
     *        necessary to create a multicast query
     * @return a new <tt>QueryRequest</tt> instance with bits set for
     *         multicast -- a min speed bit in particular
     * @throws <tt>NullPointerException</tt> if the <tt>qr</tt> argument is
     *         <tt>null</tt>
     */
    public QueryRequest createMulticastQuery(byte[] guid, QueryRequest qr);

    /**
     * Creates a new <tt>QueryRequest</tt> that is a copy of the input query,
     * except that it includes the specified query key.
     * 
     * @param qr the <tt>QueryRequest</tt> to use
     * @param key the <tt>AddressSecurityToken</tt> to add
     * @return a new <tt>QueryRequest</tt> from the specified query and key
     */
    public QueryRequest createQueryKeyQuery(QueryRequest qr,
            AddressSecurityToken key);

    /**
     * Specialized constructor used to create a query without the firewalled bit
     * set. This should primarily be used for testing.
     * 
     * @param query the query string
     * @return a new <tt>QueryRequest</tt> with the specified query string and
     *         without the firewalled bit set
     */
    public QueryRequest createNonFirewalledQuery(String query, byte ttl);

    /**
     * Creates a new query from the network.
     * 
     * @param guid the GUID of the query
     * @param ttl the time to live of the query
     * @param hops the hops of the query
     * @param payload the query payload
     * 
     * @return a new <tt>QueryRequest</tt> instance from the specified data
     */
    public QueryRequest createNetworkQuery(byte[] guid, byte ttl, byte hops,
            byte[] payload, Network network) throws BadPacketException;

    /**
     * Builds a new query from scratch but you can flag it as a Requery, if
     * needed. If you need to make a query that accepts out-of-band results, be
     * sure to set the guid correctly (see GUID.makeAddressEncodedGUI) and set
     * canReceiveOutOfBandReplies .
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     * @param queryUrns <code>Set</code> of <code>URN</code> instances requested for
     *        this query, which may be empty or null if no URNs were requested
     * @throws <tt>IllegalArgumentException</tt> if the query string, the xml
     *         query string, and the urns are all empty, or if the feature
     *         selector is bad
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, String query,
            String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector);

    /**
     * Builds a new query from scratch but you can flag it as a Requery, if
     * needed. If you need to make a query that accepts out-of-band results, be
     * sure to set the guid correctly (see GUID.makeAddressEncodedGUI) and set
     * canReceiveOutOfBandReplies .
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     * @param queryUrns <code>Set</code> of <code>URN</code> instances requested for
     *        this query, which may be empty or null if no URNs were requested
     * @throws <tt>IllegalArgumentException</tt> if the query string, the xml
     *         query string, and the urns are all empty, or if the feature
     *         selector is bad
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, String query,
            String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector, boolean doNotProxy, int metaFlagMask);

    /**
     * Builds a new query from scratch but you can flag it as a Requery, if
     * needed. If you need to make a query that accepts out-of-band results, be
     * sure to set the guid correctly (see GUID.makeAddressEncodedGUI) and set
     * canReceiveOutOfBandReplies .
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     * @param queryUrns <tt>Set</tt> of <tt>URN</tt> instances requested for
     *        this query, which may be empty or null if no URNs were requested
     * @throws <tt>IllegalArgumentException</tt> if the query string, the xml
     *         query string, and the urns are all empty, or if the capability
     *         selector is bad
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, int minSpeed,
            String query, String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector, boolean doNotProxy, int metaFlagMask);

    /**
     * Builds a new query from scratch but you can flag it as a Requery, if
,     * needed. If you need to make a query that accepts out-of-band results, be
     * sure to set the guid correctly (see GUID.makeAddressEncodedGUI) and set
     * canReceiveOutOfBandReplies .
     * 
     * @requires 0<=minSpeed<2^16 (i.e., can fit in 2 unsigned bytes)
     * @param queryUrns <code>Set</code> of <code>URN</code> instances requested for
     *        this query, which may be empty or null if no URNs were requested
     * @throws <tt>IllegalArgumentException</tt> if the query string, the xml
     *         query string, and the urns are all empty, or if the capability
     *         selector is bad
     */
    public QueryRequest createQueryRequest(byte[] guid, byte ttl, int minSpeed,
            String query, String richQuery, Set<? extends URN> queryUrns,
            AddressSecurityToken addressSecurityToken, boolean isFirewalled,
            Network network, boolean canReceiveOutOfBandReplies,
            int featureSelector, boolean doNotProxy, int metaFlagMask,
            boolean normalize);

}