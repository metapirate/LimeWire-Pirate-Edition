package com.limegroup.gnutella.filters.response;

import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * A filter that disallows responses with alt-locs that are blocked by the
 * IP filter.
 */
@Singleton
public class AltLocFilter implements ResponseFilter {

    private final IPFilter ipFilter;

    @Inject
    AltLocFilter(IPFilter ipFilter) {
        this.ipFilter = ipFilter;
    }

    public boolean allow(QueryReply qr, Response response) {
        for(IpPort alt : response.getLocations()) {
            if(!ipFilter.allow(alt.getInetAddress().getAddress()))
                return false;
        }
        return true;
    }
}
