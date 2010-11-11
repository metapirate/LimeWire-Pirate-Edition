package com.limegroup.gnutella.filters;

import org.limewire.core.settings.FilterSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * A filter that blocks query requests with the same query and TTL as recently
 * seen query requests. The TTL check prevents the filter from interfering with
 * dynamic querying.
 */
@Singleton
public class RepetitiveQueryFilter implements SpamFilter {

    private static final Log LOG = LogFactory.getLog(RepetitiveQueryFilter.class);

    /** Recent incoming queries. */
    private final String[] recentQueries;

    /** The TTLs of the recent queries. */
    private final byte[] recentTTLs;

    /** The index of the recent query that should be replaced next. */
    private int roundRobin = 0;

    /** The number of repetitive queries dropped this session. */
    private long dropped = 0;

    private final ConnectionManager connectionManager;

    @Inject
    RepetitiveQueryFilter(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        int size = FilterSettings.REPETITIVE_QUERY_FILTER_SIZE.getValue();
        recentQueries = new String[size];
        recentTTLs = new byte[size];
    }

    @Override
    public boolean allow(Message m) {
        if(!(m instanceof QueryRequest))
            return true;
        if(recentQueries.length == 0)
            return true;
        if(connectionManager.isShieldedLeaf())
            return true;
        QueryRequest q = (QueryRequest)m;
        // Don't drop browses or "what's new" queries
        if(q.isBrowseHostQuery() || q.isWhatIsNewRequest())
            return true;
        String query = q.getQuery();
        assert query != null;
        // Don't drop URN queries (though they may be dropped elsewhere)
        if(query.equals(QueryRequest.DEFAULT_URN_QUERY) && q.hasQueryUrns())
            return true;
        byte ttl = q.getTTL();
        // Drop repetitive queries
        for(int i = 0; i < recentQueries.length; i++) {
            if(query.equals(recentQueries[i]) && ttl == recentTTLs[i]) {
                LOG.debugf("Repetitive query blocked: {0}, {1}", query, ttl);
                dropped++;
                return false;
            }
        }
        recentQueries[roundRobin] = query;
        recentTTLs[roundRobin] = ttl;
        roundRobin = (roundRobin + 1) % recentQueries.length;
        return true;
    }
}
