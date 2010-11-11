package com.limegroup.gnutella.filters;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.limegroup.gnutella.messages.Message;

public class CompositeFilter implements SpamFilter {

    private static Log LOG = LogFactory.getLog(CompositeFilter.class);

    SpamFilter[] delegates;

    /**
     * @requires filters not modified while this is in use (rep is exposed!),
     *           filters contains no null elements
     * @effects creates a new spam filter from a number of other filters.
     */
    public CompositeFilter(SpamFilter[] filters) {
        this.delegates = filters;
    }

    public boolean allow(Message m) {
        for(int i = 0; i < delegates.length; i++) {
            if(!delegates[i].allow(m)) {
                String name = delegates[i].getClass().getSimpleName();
                LOG.debugf("{0} blocked {1}", name, m);
                return false;
            }
        }
        return true;
    }
}
