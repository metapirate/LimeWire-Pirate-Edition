package com.limegroup.gnutella.filters;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.settings.FilterSettings;
import org.limewire.util.Visitor;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * A filter that checks query responses, query replies and individual URNs
 * against a URN blacklist.
 */
@Singleton
class URNFilterImpl implements URNFilter {

    private static final Log LOG = LogFactory.getLog(URNFilterImpl.class);

    private final SpamManager spamManager;
    private final URNBlacklistManager urnBlacklistManager;
    private final ScheduledExecutorService backgroundExecutor;
    private ImmutableSet<String> blacklist = null;

    @Inject
    URNFilterImpl(SpamManager spamManager,
            URNBlacklistManager urnBlacklistManager,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.spamManager = spamManager;
        this.urnBlacklistManager = urnBlacklistManager;
        this.backgroundExecutor = backgroundExecutor;
    }

    /**
     * Reloads the blacklist in a different thread and informs the callback,
     * unless the callback is null.
     */
    @Override
    public void refreshURNs(final LoadCallback callback) {
        LOG.debug("Refreshing URN filter");
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final ImmutableSet.Builder<String> builder =
                    ImmutableSet.builder();
                // 1. Local setting
                for(String s : FilterSettings.FILTERED_URNS_LOCAL.get())
                    builder.add(s);
                // 2. Remote setting
                if(FilterSettings.USE_NETWORK_FILTER.getValue()) {
                    for(String s : FilterSettings.FILTERED_URNS_REMOTE.get())
                        builder.add(s);
                }
                // 3. File
                urnBlacklistManager.loadURNs(new Visitor<String>() {
                    @Override
                    public boolean visit(String s) {
                        builder.add(s);
                        return true;
                    }
                });
                blacklist = builder.build();
                if(LOG.isDebugEnabled())
                    LOG.debug("Filter contains " + blacklist.size() + " URNs");
                if(callback != null)
                    callback.spamFilterLoaded();
            }
        });
    }

    /**
     * Returns false if the message is a query reply with a URN that matches
     * the blacklist; matching query replies are passed to the spam filter.
     * Browse host replies are always allowed. Returns true for all other
     * messages.
     */
    @Override
    public boolean allow(Message m) {
        if(blacklist == null)
            return true;
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            if(q.isBrowseHostReply())
                return true; // We'll filter individual responses later
            if(isBlacklisted(q)) {
                if(FilterSettings.FILTERED_URNS_ARE_SPAM.getValue())
                    spamManager.handleSpamQueryReply(q);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns false if the query reply is a browse host reply and the response
     * contains a URN that matches the blacklist; otherwise returns true.
     */
    @Override
    public boolean allow(QueryReply q, Response r) {
        if(q.isBrowseHostReply()) {
            for(URN u : r.getUrns()) {
                if(isBlacklisted(u))
                    return false;
            }
        }
        return true;
    }
    

    @Override
    public boolean allow(SearchResult result, LimeXMLDocument document) {
        
        if (isBlacklisted((URN)result.getUrn())) {
            return false;
        }
    
        return true;
    }

    /**
     * Returns true if any response in the query reply matches the blacklist.
     * Unlike <code>allow(Message)</code>, matching query replies are not
     * passed to the spam filter.
     */
    @Override
    public boolean isBlacklisted(QueryReply q) {
        if(blacklist == null)
            return false;
        try {
            for(Response r : q.getResultsArray()) {
                for(URN u : r.getUrns()) {
                    if(isBlacklisted(u))
                        return true;
                }
                LimeXMLDocument doc = r.getDocument();
                if(doc != null) {
                    String infohash = doc.getValue(LimeXMLNames.TORRENT_INFO_HASH);
                    if(infohash != null && isBlacklisted(infohash))
                        return true;
                }
            }
            return false;
        } catch(BadPacketException bpe) {
            return true;
        }
    }

    /**
     * Returns true if the given URN matches the blacklist.
     */
    @Override
    public boolean isBlacklisted(URN urn) {
        if(blacklist == null || urn == null)
            return false;
        return isBlacklisted(urn.getNamespaceSpecificString());
    }

    /**
     * Returns true if the given string representation of a URN matches the
     * blacklist.
     */
    private boolean isBlacklisted(String urn) {
        if(blacklist.contains(urn)) {
            if(LOG.isDebugEnabled())
                LOG.debug(urn + " is spam");
            return true;
        }
        return false;
    }

    /**
     * Returns the blacklisted URNs as base32-encoded strings. For testing.
     */
    @Override
    public Set<String> getBlacklist() {
        return blacklist;
    }

}