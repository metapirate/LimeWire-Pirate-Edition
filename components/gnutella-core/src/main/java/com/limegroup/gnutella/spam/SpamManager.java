package com.limegroup.gnutella.spam;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Calculates spam ratings for search results based on their similarity to
 * previous results that have been marked, either manually or automatically, as
 * spam or not spam. 
 */
public interface SpamManager {

    /**
     * Returns the spam manager's rating table. For testing.
     */
    public RatingTable getRatingTable();

    /**
     * Clears bad ratings for the keywords in a query started by the user.
     * 
     * @param qr the QueryRequest started by the user
     */
    public void startedQuery(QueryRequest qr);

    /**
     * Calculates, sets and returns the spam rating for a RemoteFileDesc.
     * 
     * @param rfd the RemoteFileDesc to rate
     * @return the spam rating of the RemoteFileDesc, between 0 (not spam) and 1
     *         (spam)
     */
    public float calculateSpamRating(RemoteFileDesc rfd);

    /**
     * Increases the spam ratings of tokens associated with a spam query reply.
     */
    public void handleSpamQueryReply(QueryReply qr);

    /**
     * Increases the spam ratings of RFDs marked by the user as being spam.
     * 
     * @param rfds an array of RemoteFileDescs that should be marked as spam
     */
    public void handleUserMarkedSpam(RemoteFileDesc[] rfds);

    /**
     * Reduces the spam ratings of RFDs marked by the user as being good.
     * 
     * @param rfds an array of RemoteFileDescs that should be marked as good
     */
    public void handleUserMarkedGood(RemoteFileDesc[] rfds);

    /**
     * Clears all collected filter data.
     */
    public void clearFilterData();
}
