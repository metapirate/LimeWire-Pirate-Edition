package com.limegroup.gnutella.http;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.limewire.collection.MultiRRIterator;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;

/**
 * Manages a list of alternate locations.
 */
public class AltLocTracker {

    /**
     * The alternate locations that have been written out (as good) locations.
     */
    private Set<DirectAltLoc> writtenLocs = new HashSet<DirectAltLoc>();

    /**
     * The firewalled alternate locations that have been written out as good
     * locations.
     */
    private Set<PushAltLoc> writtenPushLocs = new HashSet<PushAltLoc>();

    /**
     * The maximum number of alts to write per http transfer.
     */
    private static final int MAX_LOCATIONS = 10;

    /**
     * The maximum number of firewalled alts to write per http transfer.
     */
    private static final int MAX_PUSH_LOCATIONS = 5;

    /**
     * The version of the FWT protocol the remote supports. Non-firewalled hosts
     * should not send this feature. INVARIANT: if this is greater than 0,
     * wantsFalts is set.
     */
    private int fwtVersion = 0;

    private final URN urn;

    private boolean wantsFAlts;

    public AltLocTracker(URN urn) {
        if (urn == null) {
            throw new IllegalArgumentException();
        }
        
        this.urn = urn;
    }

    /**
     * Returns an AlternateLocationCollection of alternates that have not been
     * sent out already.
     */
    public Collection<DirectAltLoc> getNextSetOfAltsToSend(AltLocManager altLocManager) {
        AlternateLocationCollection<DirectAltLoc> coll = altLocManager.getDirect(urn);
        Collection<DirectAltLoc> ret = null;
        long now = System.currentTimeMillis();
        synchronized (coll) {
            Iterator<DirectAltLoc> iter = coll.iterator();
            for (int i = 0; iter.hasNext() && i < MAX_LOCATIONS;) {
                DirectAltLoc al = iter.next();
                if (writtenLocs.contains(al))
                    continue;

                if (al.canBeSent(AlternateLocation.MESH_LEGACY)) {
                    writtenLocs.add(al);
                    if (ret == null)
                        ret = new HashSet<DirectAltLoc>();
                    ret.add(al);
                    i++;
                    al.send(now, AlternateLocation.MESH_LEGACY);
                } else if (!al.canBeSentAny())
                    iter.remove();
            }
        }
        if (ret == null)
            return Collections.emptySet();
        else
            return ret;

    }

    public Collection<PushAltLoc> getNextSetOfPushAltsToSend(AltLocManager altLocManager) {
        if (!wantsFAlts)
            return Collections.emptySet();

        AlternateLocationCollection<PushAltLoc> fwt = altLocManager.getPushFWT(urn);

        AlternateLocationCollection<PushAltLoc> push;
        if (fwtVersion > 0)
            push = AlternateLocationCollection.getEmptyCollection();
        else
            push = altLocManager.getPushNoFWT(urn);

        Collection<PushAltLoc> ret = null;
        long now = System.currentTimeMillis();
        synchronized (push) {
            synchronized (fwt) {
                Iterator<PushAltLoc> iter = new MultiRRIterator<PushAltLoc>(fwt
                        .iterator(), push.iterator());
                for (int i = 0; iter.hasNext() && i < MAX_PUSH_LOCATIONS;) {
                    PushAltLoc al = iter.next();

                    if (writtenPushLocs.contains(al))
                        continue;

                    // it is possible to end up having a PE with all
                    // proxies removed. In that case we remove it explicitly
                    if (al.getPushAddress().getProxies().isEmpty()) {
                        iter.remove();
                        continue;
                    }

                    if (al.canBeSent(AlternateLocation.MESH_LEGACY)) {
                        al.send(now, AlternateLocation.MESH_LEGACY);
                        writtenPushLocs.add(al);

                        if (ret == null)
                            ret = new HashSet<PushAltLoc>();
                        ret.add(al);
                        i++;
                    } else if (!al.canBeSentAny())
                        iter.remove();
                }
            }
        }

        if (ret == null)
            return Collections.emptySet();
        else
            return ret;
    }

    public void addLocation(AlternateLocation al) {
        if (al instanceof DirectAltLoc)
            writtenLocs.add((DirectAltLoc) al);
        else
            writtenPushLocs.add((PushAltLoc) al); // no problem if we add an
                                                    // existing pushloc
    }

    public boolean wantsFAlts() {
        return wantsFAlts;
    }

    public void setWantsFAlts(boolean wantsFAlts) {
        this.wantsFAlts = wantsFAlts;
    }

    public URN getUrn() {
        return urn;
    }

    public int getFwtVersion() {
        return fwtVersion;
    }

    public void setFwtVersion(int fwtVersion) {
        this.fwtVersion = fwtVersion;
    }

}
