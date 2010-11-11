package com.limegroup.gnutella;



import org.limewire.core.api.search.SearchCategory;

import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Records information about queries so that responses can be validated later.
 * Typical use is to call record(..) on an outgoing query request, and
 * score/matchesType/isMandragoreWorm on each incoming response.  
 */
public interface ResponseVerifier {
    
    /** Same as record(qr, null). */
    public void record(QueryRequest qr);

    /**
     *  @modifies this
     *  @effects memorizes the query string for qr; this will be used to score
     *   responses later.  If type!=null, also memorizes that qr was for the given
     *   media type; otherwise, this is assumed to be for any type.
     */
    public void record(QueryRequest qr, SearchCategory type);

    public boolean matchesQuery(byte [] guid, Response response);
    

    /**
     * Returns true if response has the same media type as the
     * corresponding query request the given GUID.  In the rare case
     * that guid is not known (because this' buffers overflowed),
     * conservatively returns true.
     */
    public boolean matchesType(byte[] guid, Response response);

    /**
     * Returns true if the given response is an instance of the Mandragore
     * Worm.  This worm responds to the query "x" with a 8KB file named
     * "x.exe".  In the rare case that the query for guid can't be found
     * returns false.
     */
    public boolean isMandragoreWorm(byte[] guid, Response response);
    
    /**
     * Returns the query string corresponding to the given query GUID, or
     * null if the GUID is unknown or has expired from the cache.
     */
    public String getQueryString(byte[] guid);
}

