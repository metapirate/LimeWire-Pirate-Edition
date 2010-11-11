package com.limegroup.gnutella.search;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.filters.response.FilterFactory;
import com.limegroup.gnutella.filters.response.ResponseFilter;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.util.ClassCNetworks;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Handles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instances and performs the logic 
 * necessary to pass those results up to the UI.
 */
@Singleton
final class SearchResultHandlerImpl implements SearchResultHandler {
    
    private static final Log LOG =
        LogFactory.getLog(SearchResultHandlerImpl.class);
        
    /**
     * The maximum amount of time to allow a query's processing
     * to pass before giving up on it as an 'old' query.
     */
    private static final int QUERY_EXPIRE_TIME = 30 * 1000; // 30 seconds.

    /**
     * The "delay" between responses to wait to send a QueryStatusResponse.
     */
    public static final int REPORT_INTERVAL = 15;

    /** 
     * The maximum number of results to send in a QueryStatusResponse -
     * basically sent to say 'shut off query'.
     */
    public static final int MAX_RESULTS = 65535;


    /** Used to keep track of the number of non-filtered responses per GUID.
     *  I need synchronization for every call I make, so a Vector is fine.
     */
    private final List<GuidCount> GUID_COUNTS = new Vector<GuidCount>();
    
    /**
     * counter for class C networks per query per urn
     * remember the last 10 queries.
     */
    private final Map<GUID, Map<URN,ClassCNetworks[]>> cncCounter = 
        Collections.synchronizedMap(new FixedsizeForgetfulHashMap<GUID, Map<URN,ClassCNetworks[]>>(10));
    
    private final NetworkManager networkManager;
    private final Provider<ActivityCallback> activityCallback;
    private final Provider<ConnectionManager> connectionManager;
    private final ConnectionServices connectionServices;
    private final Provider<SpamManager> spamManager;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final QuerySettings querySettings;
    
    private volatile ResponseFilter responseFilter;

    private final PushEndpointFactory pushEndpointFactory;

    @Inject
    public SearchResultHandlerImpl(NetworkManager networkManager,
            Provider<ActivityCallback> activityCallback,
            Provider<ConnectionManager> connectionManager,
            ConnectionServices connectionServices,
            Provider<SpamManager> spamManager,
            RemoteFileDescFactory remoteFileDescFactory,
            NetworkInstanceUtils networkInstanceUtils,
            PushEndpointFactory pushEndpointFactory,
            FilterFactory responseFilterFactory,
            QuerySettings querySettings) {
        this.networkManager = networkManager;
        this.activityCallback = activityCallback;
        this.connectionManager = connectionManager;
        this.connectionServices = connectionServices;
        this.spamManager = spamManager;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.networkInstanceUtils = networkInstanceUtils;
        this.pushEndpointFactory = pushEndpointFactory;
        this.responseFilter = responseFilterFactory.createResponseFilter();
        this.querySettings = querySettings;
    }

    @Override
    public void setResponseFilter(ResponseFilter responseFilter) {
        this.responseFilter = responseFilter;
    }

    /**
     * Adds the Query to the list of queries kept track of. You should do this
     * EVERY TIME you start a query so we can leaf guide it when possible. Also
     * adds the query to the Spam Manager to adjust percentages.
     * 
     * @param qr The query that has been started. We really just acces the guid.
     */ 
    public void addQuery(QueryRequest qr) {
        LOG.trace("entered SearchResultHandler.addQuery(QueryRequest)");
        if (!qr.isBrowseHostQuery() && !qr.isWhatIsNewRequest())
            spamManager.get().startedQuery(qr);
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.add(gc);
    }

    /**
     * Removes the Query frome the list of queries kept track of.  You should do
     * this EVERY TIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    public void removeQuery(GUID guid) {
        LOG.trace("entered SearchResultHandler.removeQuery(GUID)");
        cncCounter.remove(guid);
        GuidCount gc = removeQueryInternal(guid);
        if ((gc != null) && (!gc.isFinished())) {
            // shut off the query at the UPs - it wasn't finished so it hasn't
            // been shut off - at worst we may shut it off twice, but that is
            // a timing issue that has a small probability of happening, no big
            // deal if it does....
            QueryStatusResponse stat = new QueryStatusResponse(guid, 
                                                               MAX_RESULTS);
            connectionManager.get().updateQueryStatus(stat);
        }
    }

    /**
     * Returns a <tt>List</tt> of queries that require replanting into
     * the network, based on the number of results they've had and/or
     * whether or not they're new enough.
     */
    public List<QueryRequest> getQueriesToReSend() {
        LOG.trace("entered SearchResultHandler.getQueriesToSend()");
        List<QueryRequest> reSend = null;
        synchronized (GUID_COUNTS) {
            long now = System.currentTimeMillis();
            for(GuidCount currGC : GUID_COUNTS) {
                if( isQueryStillValid(currGC, now) ) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("adding " + currGC + 
                                  " to list of queries to resend");
                    if( reSend == null )
                        reSend = new LinkedList<QueryRequest>();
                    reSend.add(currGC.getQueryRequest());
                }
            }
        }
        if( reSend == null )
            return Collections.emptyList();
        else
            return reSend;
    }        


    /**
     * Use this to see how many results have been displayed to the user for the
     * specified query.
     *
     * @param guid the guid of the query.
     *
     * @return the number of non-filtered results for query with guid guid. -1
     * is returned if the guid was not found....
     */    
    public int getNumResultsForQuery(GUID guid) {
        GuidCount gc = retrieveGuidCount(guid);
        if (gc != null)
            return gc.getNumResults();
        else
            return -1;
    }
    
    /*---------------------------------------------------    
      END OF PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /*---------------------------------------------------    
      PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/


    /** 
     * Handles the given query reply. Only one thread may call it at a time.
     *      
     * @return <tt>true</tt> if the GUI will (probably) display the results,
     *  otherwise <tt>false</tt> 
     */
    public void handleQueryReply(final QueryReply qr, Address address) {
        try {
            qr.validate();
        } catch(BadPacketException bpe) {
            LOG.debug("Ignoring corrupt query reply", bpe);
            return;
        }

        // always handle reply to multicast queries.
        if(!qr.isReplyToMulticastQuery() && !qr.isBrowseHostReply()) {
            // Drop the reply if there's no way to connect to the responder.
            if(qr.calculateQualityOfService()
                    < SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
                LOG.debug("Ignoring reply with low quality");
                return;
            }
            if(qr.getSpeed()
                    < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                LOG.debug("Ignoring reply with low speed");
                return;
            }
            // TODO: the logic below belongs in qr.calculateQualityOfService()
            if(qr.isFirewalled()) {
                LOG.debug("The responder is firewalled");
                if(!networkInstanceUtils.isVeryCloseIP(qr.getIPBytes())) {
                    LOG.debug("...and the responder isn't on a very close IP");
                    boolean weAreFirewalled =
                        !networkManager.acceptedIncomingConnection();
                    if(weAreFirewalled)
                        LOG.debug("...and we're firewalled");
                    byte[] ourAddress = networkManager.getAddress();
                    boolean weArePrivate =
                        networkInstanceUtils.isPrivateAddress(ourAddress);
                    if(weArePrivate)
                        LOG.debug("...and we have a private IP");
                    if(weAreFirewalled || weArePrivate) {
                        boolean weCanDoFWT = networkManager.canDoFWT();
                        if(!weCanDoFWT)
                            LOG.debug("...and we can't do FWT");
                        boolean theyCanDoFWT = qr.getSupportsFWTransfer();
                        if(!theyCanDoFWT)
                            LOG.debug("...and the responder can't do FWT");
                        if(!(weCanDoFWT && theyCanDoFWT)) {
                            LOG.debug("...so we're ignoring the reply");
                            return;
                        }
                    }
                }
            }
        }

        List<Response> results = null;
        try {
            results = qr.getResultsAsList();
        } catch (BadPacketException e) {
            LOG.debug("Error getting results", e);
            return;
        }

        // throw away results that aren't secure.
        Status secureStatus = qr.getSecureStatus();
        if(secureStatus == Status.FAILED) {
            LOG.debug("Ignoring secure result that failed verification");
            return;
        }
        
        boolean skipSpam = isWhatIsNew(qr) || qr.isBrowseHostReply();
        int numGoodSentToFrontEnd = 0;
            
        float spamThreshold = 1;
        if(SearchSettings.ENABLE_SPAM_FILTER.getValue())
            spamThreshold = SearchSettings.FILTER_SPAM_RESULTS.getValue();
        
        for(Response response : results) {
            if(!responseFilter.allow(qr, response)) {
                LOG.debug("Ignoring result because of response filter");
                continue;
            }
            
            // Filter responses with no URNs, unless they're secure results
            if(secureStatus != Status.SECURE && response.getUrns().isEmpty()) {
                LOG.debug("Ignoring insecure result with no URNs");
                continue;
            }
            
            // If there was an action, only allow it if it's a secure message.
            if(secureStatus != Status.SECURE
                    && ApplicationSettings.USE_SECURE_RESULTS.getValue()) {
                LimeXMLDocument doc = response.getDocument();
                if(doc != null && !"".equals(doc.getAction())) {
                    LOG.debug("Ignoring insecure result with XML action");
                    continue;
                }
            }
            
            // we'll be showing the result to the user, count it
            countClassC(qr,response);
            RemoteFileDesc rfd;
            try {
                rfd = response.toRemoteFileDesc(qr, address, remoteFileDescFactory, pushEndpointFactory);
            } catch (UnknownHostException e) {
                throw new RuntimeException("should not have happened", e);
            }
            rfd.setSecureStatus(secureStatus);
            Set<? extends IpPort> alts = response.getLocations();
            
            // Set the spam rating for the RemoteFileDesc
            float spamRating = spamManager.get().calculateSpamRating(rfd);
            
            // Count non-spam results for dynamic querying
            if(skipSpam || spamRating < spamThreshold)
                numGoodSentToFrontEnd++;
            
            // Send the result to the UI
            activityCallback.get().handleQueryResult(rfd, qr, alts);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug(numGoodSentToFrontEnd + " responses sent to the UI");
        
        accountAndUpdateDynamicQueriers(qr, numGoodSentToFrontEnd);
    }

    private void countClassC(QueryReply qr, Response r) {
        synchronized(cncCounter) {
            GUID searchGuid = new GUID(qr.getGUID());
            Map<URN, ClassCNetworks[]> m = cncCounter.get(searchGuid);
            if (m == null) {
                m = new HashMap<URN,ClassCNetworks[]>();
                cncCounter.put(searchGuid,m);
            }
            for (URN u : r.getUrns()) {
                ClassCNetworks [] cnc = m.get(u);
                if (cnc == null) {
                    cnc = new ClassCNetworks[]{new ClassCNetworks(), new ClassCNetworks()};
                    m.put(u, cnc);
                }
                cnc[0].add(ByteUtils.beb2int(qr.getIPBytes(), 0), 1);
                cnc[1].addAll(r.getLocations());
            }
        }
    }

    private void accountAndUpdateDynamicQueriers(final QueryReply qr,
                                                 final int numGoodSentToFrontEnd) {

        // we should execute if results were consumed
        if (numGoodSentToFrontEnd > 0) {
            // get the correct GuidCount
            GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gc == null)
                // 0. probably just hit lag, or....
                // 1. we could be under attack - hits not meant for us
                // 2. programmer error - ejected a query we should not have
                return;
            
            // update the object
            gc.increment(numGoodSentToFrontEnd);

            // inform proxying Ultrapeers....
            if (connectionServices.isShieldedLeaf()) {
                if (!gc.isFinished() && 
                    (gc.getNumResults() > gc.getNextReportNum())) {
                    gc.tallyReport();
                    if (gc.getNumResults() > querySettings.getUltrapeerResults())
                        gc.markAsFinished();
                    // if you think you are done, then undeniably shut off the
                    // query.
                    final int numResultsToReport = (gc.isFinished() ?
                                                    MAX_RESULTS :
                                                    gc.getNumResults()/4);
                    QueryStatusResponse stat = 
                        new QueryStatusResponse(gc.getGUID(), 
                                                numResultsToReport);
                    connectionManager.get().updateQueryStatus(stat);
                }

            }
        }
    }


    private GuidCount removeQueryInternal(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterator<GuidCount> iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = iter.next();
                if (currGC.getGUID().equals(guid)) {
                    iter.remove();  // get rid of this dude
                    return currGC;  // and return it...
                }
            }
        }
        return null;
    }


    private GuidCount retrieveGuidCount(GUID guid) {
        synchronized (GUID_COUNTS) {
            for(GuidCount currGC : GUID_COUNTS) {
                if (currGC.getGUID().equals(guid))
                    return currGC;
            }
        }
        return null;
    }
    
    private boolean isWhatIsNew(QueryReply reply) {
        GuidCount gc = retrieveGuidCount(new GUID(reply.getGUID()));
        return gc != null && gc.getQueryRequest().isWhatIsNewRequest();
    }
    
    /**
     * Determines whether or not the query contained in the
     * specified GuidCount is still valid.
     * This depends on values such as the time the query was
     * created and the amount of results we've received so far
     * for this query.
     */
    private boolean isQueryStillValid(GuidCount gc, long now) {
        LOG.trace("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (gc.getTime() + QUERY_EXPIRE_TIME)) &&
               (gc.getNumResults() < querySettings.getUltrapeerResults());
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
    /** A container that simply pairs a GUID and an int.  The int should
     *  represent the number of non-filtered results for the GUID.
     */
    private static class GuidCount {

        private final long _time;
        private final GUID _guid;
        private final QueryRequest _qr;
        private int _numGoodResults;
        private int _nextReportNum = REPORT_INTERVAL;
        private boolean markAsFinished = false;
        
        public GuidCount(QueryRequest qr) {
            _qr = qr;
            _guid = new GUID(qr.getGUID());
            _time = System.currentTimeMillis();
        }

        public GUID getGUID() { return _guid; }
        public int getNumResults() {
            return _numGoodResults ;
        }
        public int getNextReportNum() { return _nextReportNum; }
        public long getTime() { return _time; }
        public QueryRequest getQueryRequest() { return _qr; }
        public boolean isFinished() { return markAsFinished; }
        public void tallyReport() { 
            _nextReportNum = _numGoodResults + REPORT_INTERVAL; 
        }

        public void increment(int good) {
            _numGoodResults += good;
        }
        
        public void markAsFinished() { markAsFinished = true; }

        @Override
        public String toString() {
            return "" + _guid + ":" + _numGoodResults + ":" + _nextReportNum;
        }
    }    
}
