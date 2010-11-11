package com.limegroup.gnutella.library;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Defines an interface to query for responses of shared files by this peer.
 */
public interface SharedFilesKeywordIndex {

    /**
     * Returns an array of all responses matching the given request.  If there
     * are no matches, the array will be empty (zero size).
     * <p>
     * Incomplete Files are returned in responses to queries that desire it.
     *
     * @return an empty array if not matching shared files were found
     */
    public abstract Response[] query(QueryRequest request);

}
