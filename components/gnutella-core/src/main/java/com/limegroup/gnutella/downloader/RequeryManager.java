package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.SearchListener;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 *  A manager for controlling how requeries are sent in downloads.
 *  This class has specific functionality that differs for
 *  Basic & PRO.
 *  <p>
 *  The manager keeps track of what queries have been sent out,
 *  when queries can begin, and how long queries should wait for results.
 *  </p>
 *  <p>
 *  It is controlled through {@link #sendQuery()}, {@link #activate()} and
 *  {@link #cleanUp()}.  
 *  </p>
 */
class RequeryManager implements DHTEventListener {

    private static final Log LOG = LogFactory.getLog(RequeryManager.class);
    
    /** The types of requeries that can be currently active. */
    public static enum QueryType { DHT, GNUTELLA };
    
    /**
     * The time to wait between requeries, in milliseconds.  This time can
     * safely be quite small because it is overridden by the global limit in
     * DownloadManager.  Package-access and non-final for testing.
     * 
     * @see com.limegroup.gnutella.DownloadManager#TIME_BETWEEN_GNUTELLA_REQUERIES 
     */
    static long TIME_BETWEEN_REQUERIES = 3L * 60L * 1000L;  //3 minutes
    
    private final RequeryListener requeryListener;
    private final DownloadManager downloadManager;
    private final AltLocFinder altLocFinder;    
    private final DHTManager dhtManager;
    private final ConnectionServices connectionServices;
    
    /** The type of the last query this sent out. */
    private volatile QueryType lastQueryType;
    
    /** The number of DHT queries already done for this downloader. */
    private volatile int numDHTQueries;
    
    /** The time the last query of either type was sent out. */
    private volatile long lastQuerySent;
    
    /** True if a gnutella query has been sent. */
    protected volatile boolean sentGnutellaQuery;
    
    /** True if requerying has been activated by the user. */
    protected volatile boolean activated;
    
    /** 
     * a dht lookup to Shutdownable if we get shut down 
     * not null if a DHT query is currently out (and not finished, success or failure)
     */
    private volatile Shutdownable dhtQuery;
    
    /**
     * Used to be notified of finished dht queries.
     * 
     * Package access for tests.
     */
    final AltLocSearchHandler searchHandler = new AltLocSearchHandler();
    
    RequeryManager(RequeryListener requeryListener, 
            DownloadManager manager,
            AltLocFinder finder,
            DHTManager dhtManager,
            ConnectionServices connectionServices) {
        this.requeryListener = requeryListener;
        this.downloadManager = manager;
        this.altLocFinder = finder;
        this.dhtManager = dhtManager;
        this.connectionServices = connectionServices;
        
        dhtManager.addEventListener(this);
    }
    
    /** Returns true if we're currently waiting for any kinds of results. */
    boolean isWaitingForResults() {
        if(lastQueryType == null)
            return false;
        
        switch(lastQueryType) {
        case DHT: return dhtQuery != null && getTimeLeftInQuery() > 0;
        case GNUTELLA: return getTimeLeftInQuery() > 0;
        }
        
        return false;
    }
    
    /** Returns the kind of last requery we sent. */
    QueryType getLastQueryType() {
        return lastQueryType;
    }
    
    /** Returns how much time, in milliseconds, is left in the current query. */
    long getTimeLeftInQuery() {
        return TIME_BETWEEN_REQUERIES - (System.currentTimeMillis() - lastQuerySent);
    }
        
    /** Sends a requery, if allowed. */
    void sendQuery() {
        if(canSendQueryNow()) {
            if(canSendDHTQueryNow())
                sendDHTQuery();
            else if(!sentGnutellaQuery)
                sendGnutellaQuery();
            else
                LOG.debug("Can send a query now, but not sending it?!");
        } else {
            LOG.debug("Tried to send query, but cannot do it now.");
        }
    }
        
    /** True if a requery can immediately be performed or can be triggered from a user action. */
    boolean canSendQueryAfterActivate() {
        return !sentGnutellaQuery || canSendDHTQueryNow();
    }
    
    /** Returns true if a query can be sent right now. */
    boolean canSendQueryNow() {
        // PRO users can always send the DHT query, but only Gnutella after activate.
        return canSendDHTQueryNow() || (activated && canSendQueryAfterActivate());
    }
    
    /** Allows activated queries to begin. */
    void activate() {
        activated = true;
    }
    
   /** Removes all event listeners, cancels ongoing searches and cleans up references. */
    void cleanUp() {
        Shutdownable f = dhtQuery;
        dhtQuery = null;
        if (f != null)
            f.shutdown();
        dhtManager.removeEventListener(this);
    }
    
    /** Specifically, this cancels the DHT query if the DHT is stopped. */
    public void handleDHTEvent(DHTEvent evt) {
        if (evt.getType() == DHTEvent.Type.STOPPED) {
            handleAltLocSearchDone(false);
            numDHTQueries = 0;
        }
    }
    
    void handleAltLocSearchDone(boolean success){
        dhtQuery = null;
        // This changes the state to GAVE_UP regardless of success,
        // because even if this was a success (it found results),
        // it's possible the download isn't going to want to use
        // those results.
        requeryListener.lookupFinished(QueryType.DHT);
    }
    
    /**
     * @return true if the dht is up and can be used for altloc queries.
     */
    private boolean isDHTUp() {
        return DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.getValue()
                && dhtManager.isMemberOfDHT();
    }
    
    /** True if another DHT query can be sent right now. */
    private boolean canSendDHTQueryNow() {
        if (!isDHTUp())
            return false;
        return numDHTQueries == 0 || 
        (numDHTQueries < DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.getValue()
                && System.currentTimeMillis() - lastQuerySent >= 
                    DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.getValue()
        );
    }
    
    private void sendDHTQuery() {
        LOG.debug("Sending a DHT requery!");
        lastQuerySent = System.currentTimeMillis();
        lastQueryType = QueryType.DHT;
        numDHTQueries++;
        requeryListener.lookupStarted(QueryType.DHT, Math.max(TIME_BETWEEN_REQUERIES, 
                LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue()));
      
        URN sha1Urn = requeryListener.getSHA1Urn();
        if (sha1Urn != null) {
            dhtQuery = altLocFinder.findAltLocs(sha1Urn, searchHandler);
        }
        // fail silently otherwise as before
    }
    
    /** Sends a Gnutella Query */
    private void sendGnutellaQuery() {
        // If we don't have stable connections, wait until we do.
        if (hasStableConnections()) {
            QueryRequest qr = requeryListener.createQuery();
            if(qr != null) {
                downloadManager.sendQuery(qr);
                LOG.debug("Sent a gnutella requery!");
                sentGnutellaQuery = true;
                lastQueryType = QueryType.GNUTELLA;
                lastQuerySent = System.currentTimeMillis();
                requeryListener.lookupStarted(QueryType.GNUTELLA, TIME_BETWEEN_REQUERIES);
            } else {
                sentGnutellaQuery = true;
                requeryListener.lookupFinished(QueryType.GNUTELLA);
            }
        } else {
            LOG.debug("Tried to send a gnutella requery, but no stable connections.");
            requeryListener.lookupPending(QueryType.GNUTELLA, CONNECTING_WAIT_TIME);
        }
    }
    
    /**
     * How long we'll wait before attempting to download again after checking
     * for stable connections (and not seeing any)
     */
    private static final int CONNECTING_WAIT_TIME     = 750;
    private static final int MIN_NUM_CONNECTIONS      = 2;
    private static final int MIN_CONNECTION_MESSAGES  = 6;
    private static final int MIN_TOTAL_MESSAGES       = 45;
            static boolean   NO_DELAY                 = false; // For testing
    
    /**
     *  Determines if we have any stable connections to send a requery down.
     */
    private boolean hasStableConnections() {
        if ( NO_DELAY )
            return true;  // For Testing without network connection

        // TODO: Note that on a private network, these conditions might
        //       be too strict.
        
        // Wait till your connections are stable enough to get the minimum 
        // number of messages
        return connectionServices.countConnectionsWithNMessages(MIN_CONNECTION_MESSAGES) 
                    >= MIN_NUM_CONNECTIONS &&
                    connectionServices.getActiveConnectionMessages() >= MIN_TOTAL_MESSAGES;
    }

    private class AltLocSearchHandler implements SearchListener<AlternateLocation> {

        public void searchFailed() {
            RequeryManager.this.handleAltLocSearchDone(false);
        }

        public void handleResult(AlternateLocation alternateLocation) {
            // ManagedDownloader installs its own AlternatelocationListener so it
            // is notified of all results
            RequeryManager.this.handleAltLocSearchDone(true);
        }
    }
}
