package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.Visitor;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.limegroup.gnutella.RemoteFileDesc;

/**
 * A ranker which uses the legacy logic for selecting from available sources.
 */
public class LegacyRanker extends AbstractSourceRanker {

    private static final Log LOG = LogFactory.getLog(LegacyRanker.class);

    private final Set<RemoteFileDescContext> rfds;

    public LegacyRanker() {
        rfds = new HashSet<RemoteFileDescContext>();
    }

    @Override
    public synchronized boolean addToPool(RemoteFileDescContext host) {
        if (LOG.isDebugEnabled())
            LOG.debug("adding host " + host + " to be ranked", new Exception());
        return rfds.add(host);
    }

    /**
     * Removes and returns the RemoteFileDesc with the highest quality in
     * filesLeft. If two or more entries have the same quality, returns the
     * entry with the highest speed.
     * 
     * @return the best file/endpoint location
     */
    @Override
    public synchronized RemoteFileDescContext getBest() {
        if (!hasMore())
            return null;

        RemoteFileDescContext ret = getBest(rfds.iterator(), getRfdVisitor());
        if(ret != null) {
            // The best rfd found so far
            boolean removed = rfds.remove(ret);
            assert removed : "unable to remove RFD.";
        }
        
        if (LOG.isDebugEnabled())
            LOG.debug("the best we came with is " + ret);

        return ret;
    }

    static RemoteFileDescContext getBest(Iterator<RemoteFileDescContext> iter, final Visitor<RemoteFileDescContext> rfdVisitor) {
        // If we were supplied a visitor, filter out invalid elements from the iterator.
        if(rfdVisitor != null) {
            iter = Iterators.filter(iter, new Predicate<RemoteFileDescContext>() {
                @Override
                public boolean apply(RemoteFileDescContext input) {
                    return rfdVisitor.visit(input);
                }
            });
        }
        
        if(!iter.hasNext()) {
            return null;
        }
        
        RemoteFileDescContext currentRfdContext  = iter.next();

        long now = System.currentTimeMillis();
        // Find max of each (remaining) element, storing in max.
        // Follows the following logic:
        // 1) Find a non-busy host (make connections)
        // 2) Find a host that uses hashes (avoid corruptions)
        // 3) Find a better quality host (avoid dud locations)
        // 4) Find a speedier host (avoid slow downloads)
        while (iter.hasNext()) {
            // define in loop to reflect current selection of ret
            RemoteFileDesc currentRfd = currentRfdContext.getRemoteFileDesc();

            RemoteFileDescContext potentialRfdContext = iter.next();
            RemoteFileDesc potentialRfd = potentialRfdContext.getRemoteFileDesc();
            
            if (potentialRfdContext.isBusy(now)) {
                continue;
            }

            if (currentRfdContext.isBusy(now)) {
                currentRfdContext = potentialRfdContext;
            }
            else if (potentialRfd.getSHA1Urn() != null && currentRfd.getSHA1Urn() == null) {
                currentRfdContext = potentialRfdContext;
            }
            // (note the use of == so that the comparison is only done
            // if both rfd & ret either had or didn't have a SHA1)
            else if ((potentialRfd.getSHA1Urn() == null) == (currentRfd.getSHA1Urn() == null)) {
                if (potentialRfd.getQuality() > currentRfd.getQuality()) {
                    currentRfdContext = potentialRfdContext;
                } else if (potentialRfd.getQuality() == currentRfd.getQuality()) {
                    if (potentialRfd.getSpeed() > currentRfd.getSpeed()) {
                        currentRfdContext = potentialRfdContext;
                    }
                }
            }
        }

        return currentRfdContext;
    }

    @Override
    public boolean hasMore() {
        return !rfds.isEmpty();
    }

    @Override
    public Collection<RemoteFileDescContext> getShareableHosts() {
        return rfds;
    }
    
    @Override
    protected boolean visitSources(Visitor<RemoteFileDescContext> contextVisitor) {
        for(RemoteFileDescContext context : rfds) {
            if(!contextVisitor.visit(context)) {
                return false;
            }
        }
        return true;
    }
}
