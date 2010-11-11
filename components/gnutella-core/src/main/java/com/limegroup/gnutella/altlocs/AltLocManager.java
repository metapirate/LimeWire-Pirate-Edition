package com.limegroup.gnutella.altlocs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.listener.EventListener;

import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileViewChangeEvent;

@Singleton
public class AltLocManager implements EventListener<FileViewChangeEvent> {

    private static final Log LOG = LogFactory.getLog(AltLocManager.class);
    
    /**
     * Map of the alternate location collections for each URN.
     * LOCKING: itself for all map operations as well as operations on the contained arrays
     */
    private final Map<URN, URNData> urnMap = Collections.synchronizedMap(new HashMap<URN, URNData>());
    
    /**
     * Adds a given altloc to the manager.
     * @return whether the manager already knew about this altloc
     */
    public boolean add(AlternateLocation al, Object source) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("alternate location added: " + al);
        }
        
        URN sha1 = al.getSHA1Urn();
        AlternateLocationCollection<DirectAltLoc> dCol = null;
        AlternateLocationCollection<PushAltLoc>   pCol = null;
        
        URNData data;
        synchronized(urnMap) {
            data = urnMap.get(sha1);
            
            if (data == null) {
                data = new URNData();
                urnMap.put(sha1,data);
            }
        }
        
        synchronized(data) {    
            if (al instanceof DirectAltLoc) { 
                if (data.direct == AlternateLocationCollection.EMPTY)
                    data.direct = AlternateLocationCollection.create(sha1);
                dCol = data.direct;
            } else if(al instanceof PushAltLoc) {
                PushAltLoc push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1) { 
                    if (data.push == AlternateLocationCollection.EMPTY)
                        data.push = AlternateLocationCollection.create(sha1);
                    pCol = data.push;
                } else { 
                    if (data.fwt == AlternateLocationCollection.EMPTY)
                        data.fwt = AlternateLocationCollection.create(sha1);
                    pCol = data.fwt;
                }
            } else {
                throw new IllegalStateException("unknown loc class: " + al.getClass());
            }
        }
        
        boolean ret = false;
        if(dCol != null) {
            ret = dCol.add((DirectAltLoc)al);
        } else if(pCol != null) {
            ret = pCol.add((PushAltLoc)al);
        } else {
            throw new IllegalStateException("didn't set a collection!");
        }
        
        // notify any listeners other than the source
        for(AltLocListener listener : data.getListeners()) {
            if (listener == source)
                continue;
            listener.locationAdded(al);
        }
        
        return ret;
    }
    
    /**
     * Removes the given altloc (implementations may demote).
     */
    public boolean remove(AlternateLocation al, Object source) {
        URN sha1 = al.getSHA1Urn();
        URNData data = urnMap.get(sha1);
        if (data == null)
            return false;

        AlternateLocationCollection<DirectAltLoc> dCol = null;
        AlternateLocationCollection<PushAltLoc>   pCol = null;
        synchronized(data) {
            if (al instanceof DirectAltLoc) { 
                dCol = data.direct;
            } else {
                PushAltLoc push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1)
                    pCol = data.push;
                else
                    pCol = data.fwt;
            }
        }

        AlternateLocationCollection col = null;
        boolean ret = false;
        if(dCol != null) {
            ret = dCol.remove((DirectAltLoc)al);
            col = dCol;
        } else if(pCol != null) {
            ret = pCol.remove((PushAltLoc)al);
            col = pCol;
        } else {
            return false;
        }
        
        // if we emptied the current collection, see if the rest are empty as well
        if (!col.hasAlternateLocations())
            removeIfEmpty(sha1,data);
        
        return ret;
    }

    private void removeIfEmpty(URN sha1, URNData data) {
        boolean empty = false;
        synchronized(data) {
            if (!data.direct.hasAlternateLocations() &&
                    !data.push.hasAlternateLocations() &&
                    !data.fwt.hasAlternateLocations() &&
                    data.getListeners().isEmpty())
                empty = true;
        }
        
        if (empty)
            urnMap.remove(sha1);
    }
    
    /**
     * @param sha1 the URN for which to get altlocs
     */
    public AlternateLocationCollection<DirectAltLoc> getDirect(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return AlternateLocationCollection.getEmptyCollection();
        
        synchronized(data) {
            return data.direct;
        }
    }
    
    /**
     * Returns push alternate locations that do not support FWT.
     * @param sha1 the URN for which to get altlocs
     */
    public AlternateLocationCollection<PushAltLoc> getPushNoFWT(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return AlternateLocationCollection.getEmptyCollection();
        
        synchronized(data) {
            return data.push;
        }
    }
    
    /**
     * Returns push alternate locations that support FWT.
     * @param sha1 the URN for which to get altlocs
     */
    public AlternateLocationCollection<PushAltLoc> getPushFWT(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return AlternateLocationCollection.getEmptyCollection();
        
        synchronized(data) {
            return data.fwt;
        }
    }
    
    public void purge(){
        urnMap.clear();
    }
    
    private void purge(URN sha1) {
        urnMap.remove(sha1);
    }
    
    public boolean hasAltlocs(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return false;
        
        return data.hasAltLocs();
    }
    
    public int getNumLocs(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return 0;
        return data.getNumLocs();
    }
    
    public void addListener(URN sha1, AltLocListener listener) {
        URNData data; 
        synchronized(urnMap){
            data = urnMap.get(sha1);
            
            if (data == null) {
                data = new URNData();
                urnMap.put(sha1,data);
            }
        }
        data.addListener(listener);
    }
    
    public void removeListener(URN sha1, AltLocListener listener) {
        URNData data =  urnMap.get(sha1);
        if (data == null)
            return;
        data.removeListener(listener);
        removeIfEmpty(sha1,data);
    }
    
    /**
     * Listens for events from FileManager.
     */
    public void handleEvent(FileViewChangeEvent evt) {
        switch(evt.getType()) {
        case FILES_CLEARED:
            purge();
            break;
        case FILE_REMOVED:
            URN urn = evt.getFileDesc().getSHA1Urn();
            // Purge if there's no more FDs for this URN.
            if(urn != null && evt.getFileView().getFileDescsMatching(urn).isEmpty()) {
                purge(urn);
            }
            break;
        }
    }

    private static class URNData {
        /** 
         * The three alternate locations we keep with this urn.
         * LOCKING: this
         */
        public AlternateLocationCollection<DirectAltLoc> direct = AlternateLocationCollection.getEmptyCollection();
        public AlternateLocationCollection<PushAltLoc> push = AlternateLocationCollection.getEmptyCollection();
        public AlternateLocationCollection<PushAltLoc> fwt = AlternateLocationCollection.getEmptyCollection();
        
        private volatile List<AltLocListener> listeners = Collections.emptyList();
        
        public synchronized boolean hasAltLocs() {
            return direct.hasAlternateLocations() || 
            push.hasAlternateLocations() || 
            fwt.hasAlternateLocations();
        }
        
        public synchronized int getNumLocs() {
            return direct.getAltLocsSize() + push.getAltLocsSize() + fwt.getAltLocsSize();
        }
        
        public synchronized void addListener(AltLocListener listener) {
            List<AltLocListener> updated = new ArrayList<AltLocListener>(listeners);
            updated.add(listener);
            listeners = updated;
        }
        
        public synchronized void removeListener(AltLocListener listener) {
            List<AltLocListener> updated = new ArrayList<AltLocListener>(listeners);
            updated.remove(listener);
            listeners = updated;
        }
        
        public List<AltLocListener> getListeners() {
            return listeners;
        }
    }
}
