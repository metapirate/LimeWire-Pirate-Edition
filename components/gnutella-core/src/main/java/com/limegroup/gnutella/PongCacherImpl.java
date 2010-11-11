package com.limegroup.gnutella;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.limewire.collection.BucketQueue;
import org.limewire.core.settings.ApplicationSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.PingReply;

/**
 * This class caches pongs from the network.  Caching pongs saves considerable
 * bandwidth because only a controlled number of pings are sent to maintain
 * adequate host data, with Ultrapeers caching and responding to pings with
 * the best pongs available.  
 */
@Singleton
public final class PongCacherImpl implements PongCacher {

    /**
     * <tt>BucketQueue</tt> holding pongs separated by hops.
     * The map is of String (locale) to BucketQueue (Pongs per Hop)
     */
    private final Map<String, BucketQueue<PingReply>> PONGS =
        new HashMap<String, BucketQueue<PingReply>>();


    private final ConnectionServices connectionServices;
    
    @Inject
    public PongCacherImpl(ConnectionServices connectionServices) {
        this.connectionServices = connectionServices;
    }

    /**
     * Accessor for testing. 
     */
    Map<String, BucketQueue<PingReply>> getPongMap() {
        return PONGS;
    }
    
    /**
     * Accessor for the <tt>Set</tt> of cached pongs.  This <tt>List</tt>
     * is unmodifiable and will throw <tt>IllegalOperationException</tt> if
     * it is modified.
     *
     * @return the <tt>List</tt> of cached pongs -- continually updated
     */
    public List<PingReply> getBestPongs(String loc) {
        synchronized(PONGS) { 
            List<PingReply> pongs = new LinkedList<PingReply>(); //list to return
            long curTime = System.currentTimeMillis();
            //first we try to populate "pongs" with those pongs
            //that match the locale 
            List<PingReply> removeList = 
                addBestPongs(loc, pongs, curTime, 0);
            //remove all stale pongs that were reported for the
            //locale
            removePongs(loc, removeList);

            //if the locale that we were searching for was not the default
            //"en" locale and we do not have enough pongs in the list
            //then populate the list "pongs" with the default locale pongs
            if(!ApplicationSettings.DEFAULT_LOCALE.get().equals(loc)
               && pongs.size() < NUM_HOPS) {

                //get the best pongs for default locale
                removeList = 
                    addBestPongs(ApplicationSettings.DEFAULT_LOCALE.get(),
                                 pongs,
                                 curTime,
                                 pongs.size());
                
                //remove any pongs that were reported as stale pongs
                removePongs(ApplicationSettings.DEFAULT_LOCALE.get(),
                            removeList);
            }

            return pongs;
        }
    }
    
    /** 
     * adds good pongs to the passed in list "pongs" and
     * return a list of pongs that should be removed.
     */
    private List<PingReply> addBestPongs(String loc, List<PingReply> pongs, 
                              long curTime, int hops) {
        //set the expire time to be used.
        //if the locale that is passed in is "en" then just use the
        //normal expire time otherwise use the longer expire time
        //so we can have some memory of non english locales
        int exp_time = 
            (ApplicationSettings.DEFAULT_LOCALE.get().equals(loc))?
            EXPIRE_TIME :
            EXPIRE_TIME_LOC;
        
        //check if there are any pongs of the specific locale stored
        //in PONGS.
        List<PingReply> remove = null;
        if(PONGS.containsKey(loc)) { 
            //get all the pongs that are of the specific locale and
            //make sure that they are not stale
            BucketQueue<PingReply> bq = PONGS.get(loc);
            Iterator<PingReply> iter = bq.iterator();
            for(;iter.hasNext() && hops < NUM_HOPS; hops++) {
                PingReply pr = iter.next();
                
                //if the pongs are stale put into the remove list
                //to be returned.  Didn't pass in the remove list
                //into this function because we may never see stale
                //pongs so we won't need to new a linkedlist
                //this may be a premature and unnecessary opt.
                if(curTime - pr.getCreationTime() > exp_time) {
                    if(remove == null) 
                        remove = new LinkedList<PingReply>();
                    remove.add(pr);
                }
                else {
                    pongs.add(pr);
                }
            }
        }
        
        return remove;
    }

    
    /**
     * removes the pongs with the specified locale and those
     * that are in the passed in list l
     */
    private void removePongs(String loc, List<PingReply> l) {
        if(l != null) {
            BucketQueue<PingReply> bq = PONGS.get(loc);
            for(PingReply pr : l) {
                bq.removeAll(pr);
            }
        }
    }                             


    /**
     * Adds the specified <tt>PingReply</tt> instance to the cache of pongs.
     *
     * @param pr the <tt>PingReply</tt> to add
     */
    public void addPong(PingReply pr) {
        // if we're not an Ultrapeer, we don't care about caching the pong
        if (!connectionServices.isSupernode())
            return;

        // Make sure we don't cache pongs that aren't from Ultrapeers.
        if(!pr.isUltrapeer()) return;      
        
        // if the hops are too high, ignore it
        if(pr.getHops() >= NUM_HOPS) return;
        synchronized(PONGS) {
            //check the map for the locale and create or retrieve the set
            if(PONGS.containsKey(pr.getClientLocale())) {
                BucketQueue<PingReply> bq = PONGS.get(pr.getClientLocale());
                bq.insert(pr, pr.getHops());
            }
            else {
                BucketQueue<PingReply> bq = new BucketQueue<PingReply>(NUM_HOPS, NUM_PONGS_PER_HOP);
                bq.insert(pr, pr.getHops());
                PONGS.put(pr.getClientLocale(), bq);
            }
        }
    }
}



