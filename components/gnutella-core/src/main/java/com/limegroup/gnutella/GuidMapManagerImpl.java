package com.limegroup.gnutella;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.io.GUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/** 
 * A factory to manage GuidMaps.
 * This will ensure that the guids are expired in an appropriate timeframe.
 */
@Singleton
class GuidMapManagerImpl implements GuidMapManager {
    
    /* The time which expired GUIDs will be purged. */
    private static long EXPIRE_POLL_TIME = 2 * 60 * 1000;
    /** The default lifetime of the GUID (10 minutes). */
    private static long TIMED_GUID_LIFETIME = 10 * 60 * 1000;
    
    /** A listing of all GuidMaps that have atleast one GUID that needs expiry. */
    private List<GuidMapImpl> toExpire = new LinkedList<GuidMapImpl>();
    /** Whether or not we've scheduled our cleaner. */
    private boolean scheduled = false;
    
    private final ScheduledExecutorService backgroundExecutor;
    
    @Inject
    public GuidMapManagerImpl(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.GuidMapFactory#getMap()
     */
    public GuidMap getMap() {
        return new GuidMapImpl();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.GuidMapFactory#removeMap(com.limegroup.gnutella.GuidMap)
     */
    public synchronized void removeMap(GuidMap expiree) {
        toExpire.remove(expiree);
    }

    /** Adds the GuidMapImpl to the list of maps that need to be expired. */
    private synchronized void addMapToExpire(GuidMapImpl expiree) {
        // schedule it on demand
        if (!scheduled) {
            backgroundExecutor.scheduleWithFixedDelay(new GuidExpirer(), 0, EXPIRE_POLL_TIME, TimeUnit.MILLISECONDS);
            scheduled = true;
        }
        toExpire.add(expiree);
    }
    
    /** Runnable that iterates through potential expirations and expires them. */
    private class GuidExpirer implements Runnable {
        public void run() {
            synchronized (GuidMapManagerImpl.this) {
                // iterator through all the maps....
                for(Iterator<GuidMapImpl> i = toExpire.iterator(); i.hasNext(); ) {
                    GuidMapImpl next = i.next();
                    synchronized (next) {
                        long now = System.currentTimeMillis();
                        Map<GUID.TimedGUID, GUID> currMap = next.getMap();
                        // and expire as many entries as possible....
                        for(Iterator<GUID.TimedGUID> j = currMap.keySet().iterator(); j.hasNext(); ) {
                            if (j.next().shouldExpire(now))
                                j.remove();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Implementation of GuidMap that delegates to the factory to expire things.
     */
    private class GuidMapImpl implements GuidMap {
        
        /** Mapping between new & old GUID.  Lazily constructed. */
        private Map<GUID.TimedGUID, GUID> map;
        
        @Override
        public String toString() {
            return "impl, map: " + map;
        }
        
        /** Returns the mapping between the two GUIDs. */
        Map<GUID.TimedGUID, GUID> getMap() {
            return map;
        }

        /** Adds a mapping from origGUID to newGUID.  The default lifetime of 10 mintues is used. */
        public void addMapping(byte[] origGUID, byte[] newGUID) {
            addMapping(origGUID, newGUID, TIMED_GUID_LIFETIME);
        }
        
        public void addMapping(byte[] origGUID, byte[] newGUID, long lifetime) {
            boolean created = false;
            synchronized(this) {
                if(map == null) {
                    map = new HashMap<GUID.TimedGUID, GUID>();
                    created = true;
                }
                
                GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(newGUID), lifetime);
                map.put(tGuid, new GUID(origGUID));
            }
            
            if(created)
                addMapToExpire(this);
        }

        public synchronized byte[] getOriginalGUID(byte[] newGUID) {
            if(map != null) {
                GUID.TimedGUID wrapper = new GUID.TimedGUID(new GUID(newGUID), 0);
                GUID orig = map.get(wrapper);
                if(orig != null)
                    return orig.bytes();
            }
            
            return null;
        }

        public synchronized GUID getNewGUID(GUID origGUID) {
            if(map != null) {
                for(Iterator<Map.Entry<GUID.TimedGUID, GUID>> i = map.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<GUID.TimedGUID, GUID> next = i.next();
                    if(next.getValue().equals(origGUID))
                        return next.getKey().getGUID();
                }
            }
            
            return null;
            
        }
        
    }
}