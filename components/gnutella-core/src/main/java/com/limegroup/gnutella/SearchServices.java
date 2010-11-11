package com.limegroup.gnutella;

import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.GUID;

public interface SearchServices {

    /** 
     * Returns true if the given response for the query with the given guid is a
     * result of the Madragore worm (8KB files of form "x.exe").  Returns false
     * if guid is not recognized.  <i>Ideally this would be done by the normal
     * filtering mechanism, but it is not powerful enough without the query
     * string.</i>
     *
     * @param guid the value returned by query(..).  MUST be 16 byts long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#isMandragoreWorm(byte[], Response) 
     */
    public boolean isMandragoreWorm(byte[] guid, Response response);

    public boolean matchesQuery(byte[] guid, Response response);

    /** 
     * Returns true if the given response is of the same type as the the query
     * with the given guid.  Returns 100 if guid is not recognized.
     *
     * @param guid the value returned by query(..).  MUST be 16 bytes long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#matchesType(byte[], Response) 
     */
    public boolean matchesType(byte[] guid, Response response);

    /** Purges the query from the QueryUnicaster (GUESS) and the ResultHandler
     *  (which maintains query stats for the purpose of leaf guidance).
     *  @param guid The GUID of the query you want to get rid of....
     */
    public void stopQuery(GUID guid);

    /**
     * Accessor for the last time a query was originated from this host.
     *
     * @return a <tt>long</tt> representing the number of milliseconds since
     *  January 1, 1970, that the last query originated from this host
     */
    public long getLastQueryTime();

    /**
     * Sends a 'What Is New' query on the network.
     * 
     * @return The SearchResultStats corresponding to the guid.
     */
    public void queryWhatIsNew(final byte[] guid, final SearchCategory type);

    /**
     * Searches the network for files with the given metadata.
     * 
     * @param richQuery metadata query to insert between the nulls,
     *  typically in XML format
     * @see query(byte[], String, MediaType)
     * 
     * @return The SearchResultStats corresponding to the guid.
     */
    public void query(final byte[] guid, final String query,
            final String richQuery, final SearchCategory  type);

    /** 
     * Searches the network for files with the given query string and 
     * minimum speed, i.e., same as query(guid, query, minSpeed, null). 
     *
     * @see query(byte[], String, MediaType)
     */
    public void query(byte[] guid, String query);

    /**
     * Searches the network for files of the given type with the given
     * GUID, query string and minimum speed.  If type is null, any file type
     * is acceptable.<p>
     *
     * ActivityCallback is notified asynchronously of responses.  These
     * responses can be matched with requests by looking at their GUIDs.  (You
     * may want to wrap the bytes with a GUID object for simplicity.)  An
     * earlier version of this method returned the reply GUID instead of taking
     * it as an argument.  Unfortunately this caused a race condition where
     * replies were returned before the GUI was prepared to handle them.
     * 
     * @param guid the guid to use for the query.  MUST be a 16-byte
     *  value as returned by newQueryGUID.
     * @param query the query string to use
     * @param minSpeed the minimum desired result speed
     * @param type the desired type of result (e.g., audio, video), or
     *  null if you don't care 
     */
    public void query(byte[] guid, String query, SearchCategory type);

    /** 
     * Returns a new GUID for passing to query.
     * This method is the central point of decision making for sending out OOB 
     * queries.
     */
    public byte[] newQueryGUID();

    /**
     * Mutates a query string by shuffling the words and removing trivial words.
     * The returned string may or may not differ from the argument.
     */
    public String mutateQuery(String query);

    /**
     * Initiates a non-blocking browse of <code>address</code> with
     * session guid <code>browseGuid</code>.
     */
    public BrowseHostHandler doAsynchronousBrowseHost(FriendPresence friendPresence, GUID browseGuid, BrowseListener browseListener);

}