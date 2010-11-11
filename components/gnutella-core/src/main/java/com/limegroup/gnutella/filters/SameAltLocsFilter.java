package com.limegroup.gnutella.filters;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;

import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.spam.SpamManager;

/**
 * Drops query replies containing multiple responses that all have similar
 * alt-locs.
 */
class SameAltLocsFilter implements SpamFilter {

    private static final Log LOG = LogFactory.getLog(SameAltLocsFilter.class);

    private final SpamManager spamManager;

    @Inject
    SameAltLocsFilter(SpamManager spamManager) {
        this.spamManager = spamManager;
    }

    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryReply) {
            Set<IpPort> sameAlts = null;
            QueryReply q = (QueryReply)m;
            try {
                Response[] responses = q.getResultsArray();
                int minResponses = FilterSettings.SAME_ALTS_MIN_RESPONSES.getValue();
                if(responses.length < minResponses) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Allowing reply with " + responses.length + " responses");
                    return true;
                }
                int minAlts = FilterSettings.SAME_ALTS_MIN_ALTS.getValue();
                float minOverlap = FilterSettings.SAME_ALTS_MIN_OVERLAP.getValue();
                for(Response r : responses) {
                    Set<? extends IpPort> alts = r.getLocations();
                    if(alts.size() < minAlts) {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Allowing reply with " + alts.size() + " alt-locs");
                        return true;
                    }
                    if(sameAlts == null) {
                        sameAlts = new IpPortSet(alts);
                    } else {
                        sameAlts.retainAll(alts);
                        if(sameAlts.size() < alts.size() * minOverlap) {
                            LOG.debug("Allowing reply with different alt-locs");
                            return true;
                        } else {
                            if(LOG.isDebugEnabled())
                                LOG.debug("Same " + sameAlts.size() + " alt-locs");
                        }
                    }
                }
                // Train the adaptive spam filter
                if(FilterSettings.SAME_ALTS_ARE_SPAM.getValue())
                    spamManager.handleSpamQueryReply(q);
                LOG.debug("Dropping reply");
                return false;
            } catch(BadPacketException bpe) {
                return false;
            }
        }
        return true;
    }
}
