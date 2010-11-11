package com.limegroup.gnutella.filters;

import java.util.Set;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.filters.response.ResponseFilter;
import com.limegroup.gnutella.filters.response.SearchResultFilter;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * A filter that checks query responses, query replies and individual URNs
 * against a URN blacklist.
 */
public interface URNFilter extends SpamFilter, ResponseFilter, SearchResultFilter {

    /**
     * Reloads the blacklist in a different thread and informs the callback,
     * unless the callback is null.
     */ 
    void refreshURNs(final LoadCallback callback);

    /**
     * Returns true if any response in the query reply matches the blacklist.
     * Unlike <code>allow(Message)</code>, matching query replies are not
     * passed to the spam filter.
     */
    boolean isBlacklisted(QueryReply q);

    /**
     * Returns true if the given URN matches the blacklist.
     */
    boolean isBlacklisted(URN urn);
    
    /**
     * Returns the blacklisted URNs as base32-encoded strings. For testing.
     */
    Set<String> getBlacklist();
}