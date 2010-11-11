package com.limegroup.gnutella.filters.response;

import java.util.Set;
import java.util.TreeSet;

import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.KeywordFilter;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * Response filter based on the GUID of the query reply. Used for
 * selectively filtering responses to "what's new" queries.
 */
@Singleton
public final class MutableGUIDFilter implements ResponseFilter {

    private static Log LOG = LogFactory.getLog(MutableGUIDFilter.class);
    
    /**
     * The Set of GUIDs to compare.
     * <p>
     * LOCKING: Never modify -- instead synchronize & replace.
     */
    private Set<byte[]> _guids =
        new TreeSet<byte[]>(new GUID.GUIDByteComparator());
    
    /**
     * The underlying filter.
     */
    private final KeywordFilter keywordFilter;
    
    MutableGUIDFilter() { 
        keywordFilter = new XMLDocFilter(true, false); 
    }
    
    MutableGUIDFilter(KeywordFilter keywordFilter) {
        this.keywordFilter = keywordFilter;
    }
    
    /**
     * Adds a guid to be scanned for keyword filters.
     */
    public synchronized void addGUID(byte[] guid) {
        if (LOG.isDebugEnabled())
            LOG.debugf("adding guid: {0}", new GUID(guid));
        Set<byte[]> guids = new TreeSet<byte[]>(new GUID.GUIDByteComparator());
        guids.addAll(_guids);
        guids.add(guid);
        _guids = guids;
    }
    
    /**
     * Removes a guid from the list of those scanned.
     */
    public void removeGUID(byte[] guid) {
        if (LOG.isDebugEnabled())
            LOG.debugf("removing guid: {0}", new GUID(guid));
        if(_guids.size() == 0) {
            return;
        } else {
            synchronized(this) {
                if(_guids.size() > 0) {
                    Set<byte[]> guids = new TreeSet<byte[]>(new GUID.GUIDByteComparator());
                    guids.addAll(_guids);
                    guids.remove(guid);
                    _guids = guids;
                }
            }
        }
    }

    @Override
    public boolean allow(QueryReply qr, Response response) {
        if(_guids.contains(qr.getGUID())) {
            if (LOG.isDebugEnabled())
                LOG.debugf("filtering for guid: {0}", new GUID(qr.getGUID()));
            boolean allowed = keywordFilter.allow(qr, response);
            if (LOG.isDebugEnabled())
                LOG.debugf("{0} allowed {1}", allowed, response);
            return allowed;
        } else {
            if (LOG.isDebugEnabled()) 
                LOG.debugf("not filtering for guid: {0}", new GUID(qr.getGUID()));
            return true;
        }
    }
}