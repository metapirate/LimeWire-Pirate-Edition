package com.limegroup.gnutella.spam;

import org.limewire.core.settings.SearchSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Calculates spam ratings for search results based on their similarity to
 * previous results that have been marked, either manually or automatically, as
 * spam or not spam. 
 */
@Singleton
class SpamManagerImpl implements SpamManager {

    private final RatingTable ratingTable;

    @Inject
    SpamManagerImpl(RatingTable ratingTable) {
        this.ratingTable = ratingTable;
    }

    /**
     * Returns the spam manager's rating table. For testing.
     */
    @Override
    public RatingTable getRatingTable() {
        return ratingTable;
    }

    /**
     * Clears bad ratings for the keywords in a query started by the user.
     * 
     * @param qr the QueryRequest started by the user
     */
    @Override
    public void startedQuery(QueryRequest qr) {
        if (SearchSettings.ENABLE_SPAM_FILTER.getValue())
            ratingTable.clear(qr);
    }

    /**
     * Calculates, sets and returns the spam rating for a RemoteFileDesc.
     * 
     * @param rfd the RemoteFileDesc to rate
     * @return the spam rating of the RemoteFileDesc, between 0 (not spam) and 1
     *         (spam)
     */
    @Override
    public float calculateSpamRating(RemoteFileDesc rfd) {
        if (!SearchSettings.ENABLE_SPAM_FILTER.getValue())
            return 0;

        float rating = 0;
        rating = 1 - (1 - rating) * (1 - ratingTable.getRating(rfd));
        rfd.setSpamRating(rating);
        return rating;
    }

    /**
     * Increases the spam ratings of tokens associated with a spam query reply.
     */
    @Override
    public void handleSpamQueryReply(QueryReply qr) {
        if (SearchSettings.ENABLE_SPAM_FILTER.getValue())
            ratingTable.rate(qr, 1);
    }

    /**
     * Increases the spam ratings of RFDs marked by the user as being spam.
     * 
     * @param rfds an array of RemoteFileDescs that should be marked as spam
     */
    @Override
    public void handleUserMarkedSpam(RemoteFileDesc[] rfds) {
        for (RemoteFileDesc rfd : rfds)
            rfd.setSpamRating(1);
        // Update the ratings of the tokens associated with the RFDs
        ratingTable.rate(rfds, 1);
    }

    /**
     * Reduces the spam ratings of RFDs marked by the user as being good.
     * 
     * @param rfds an array of RemoteFileDescs that should be marked as good
     */
    @Override
    public void handleUserMarkedGood(RemoteFileDesc[] rfds) {
        for (RemoteFileDesc rfd : rfds)
            rfd.setSpamRating(0);
        // Update the ratings of the tokens associated with the RFDs
        ratingTable.rate(rfds, 0);
    }

    /**
     * Clears all collected filter data.
     */
    @Override
    public void clearFilterData() {
        ratingTable.clear();
    }
}
