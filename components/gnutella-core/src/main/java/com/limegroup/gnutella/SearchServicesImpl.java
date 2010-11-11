package com.limegroup.gnutella;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.GUID;
import org.limewire.util.DebugRunnable;
import org.limewire.util.I18NConvert;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.response.MutableGUIDFilter;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;
import com.limegroup.gnutella.statistics.QueryStats;
import com.limegroup.gnutella.util.QueryUtils;

@Singleton
public class SearchServicesImpl implements SearchServices {
    
    private final Provider<ResponseVerifier> responseVerifier;
    private final Provider<QueryUnicaster> queryUnicaster;
    private final Provider<SearchResultHandler> searchResultHandler;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<ConnectionServices> connectionServices;
    private final Provider<QueryDispatcher> queryDispatcher;
    private final Provider<MutableGUIDFilter> mutableGUIDFilter;
    private final Provider<QueryStats> queryStats; 
    private final Provider<NetworkManager> networkManager;
    private final Provider<QueryRequestFactory> queryRequestFactory;
    private final BrowseHostHandlerManager browseHostHandlerManager;
    private final OutOfBandStatistics outOfBandStatistics;

    @Inject
    public SearchServicesImpl(Provider<ResponseVerifier> responseVerifier,
            Provider<QueryUnicaster> queryUnicaster,
            Provider<SearchResultHandler> searchResultHandler,
            Provider<MessageRouter> messageRouter,
            Provider<ConnectionServices> connectionServices,
            Provider<QueryDispatcher> queryDispatcher,
            Provider<MutableGUIDFilter> mutableGUIDFilter,
            Provider<QueryStats> queryStats,
            Provider<NetworkManager> networkManager,
            Provider<QueryRequestFactory> queryRequestFactory,
            BrowseHostHandlerManager browseHostHandlerManager,
            OutOfBandStatistics outOfBandStatistics) {
        this.responseVerifier = responseVerifier;
        this.queryUnicaster = queryUnicaster;
        this.searchResultHandler = searchResultHandler;
        this.messageRouter = messageRouter;
        this.connectionServices = connectionServices;
        this.queryDispatcher = queryDispatcher;
        this.mutableGUIDFilter = mutableGUIDFilter;
        this.queryStats = queryStats;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.browseHostHandlerManager = browseHostHandlerManager;
        this.outOfBandStatistics = outOfBandStatistics;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#isMandragoreWorm(byte[], com.limegroup.gnutella.Response)
     */
    public boolean isMandragoreWorm(byte[] guid, Response response) {
        return responseVerifier.get().isMandragoreWorm(guid, response);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#matchesQuery(byte[], com.limegroup.gnutella.Response)
     */
    public boolean matchesQuery(byte [] guid, Response response) {
        return responseVerifier.get().matchesQuery(guid, response);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#matchesType(byte[], com.limegroup.gnutella.Response)
     */
    public boolean matchesType(byte[] guid, Response response) {
        return responseVerifier.get().matchesType(guid, response);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#stopQuery(com.limegroup.gnutella.GUID)
     */
    public void stopQuery(GUID guid) {
        queryUnicaster.get().purgeQuery(guid);
        searchResultHandler.get().removeQuery(guid);
        messageRouter.get().queryKilled(guid);
        if(connectionServices.get().isSupernode())
            queryDispatcher.get().addToRemove(guid);
        mutableGUIDFilter.get().removeGUID(guid.bytes());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#getLastQueryTime()
     */
    public long getLastQueryTime() {
    	return queryStats.get().getLastQueryTime();
    }

    /**
     * Just aggregates some common code in query() and queryWhatIsNew().
     * 
     * @param qr the search request 
     * @param type
     * @return The new stats object for this query.
     */
    private void recordAndSendQuery(final QueryRequest qr, 
                                           final SearchCategory type) {
        queryStats.get().recordQuery();
        responseVerifier.get().record(qr, type);
        searchResultHandler.get().addQuery(qr); // so we can leaf guide....
        messageRouter.get().sendDynamicQuery(qr);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#queryWhatIsNew(byte[], com.limegroup.gnutella.MediaType)
     */
    public void queryWhatIsNew(final byte[] guid, final SearchCategory type) {
            QueryRequest qr = null;
            if (GUID.addressesMatch(guid, networkManager.get().getAddress(), networkManager.get().getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = queryRequestFactory.get().createWhatIsNewOOBQuery(guid, (byte)2, type);
                outOfBandStatistics.addSentQuery();
            } else {
                qr = queryRequestFactory.get().createWhatIsNewQuery(guid, (byte)2, type);
            }
    
            if(FilterSettings.FILTER_ADULT.getValue())
                mutableGUIDFilter.get().addGUID(guid);
    
            recordAndSendQuery(qr, type);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#query(byte[], java.lang.String, java.lang.String, com.limegroup.gnutella.MediaType)
     */
    public void query(final byte[] guid, 
    						 String query, 
    						 final String richQuery, 
    						 final SearchCategory type) {
            QueryRequest qr = null;
            query = QueryUtils.removeIllegalChars(query);
            query = I18NConvert.instance().getNorm(query);
            if(query.length() > 0) {
                if (networkManager.get().isIpPortValid() && (new GUID(guid)).addressesMatch(networkManager.get().getAddress(), 
                        networkManager.get().getPort())) {
                    // if the guid is encoded with my address, mark it as needing out
                    // of band support.  note that there is a VERY small chance that
                    // the guid will be address encoded but not meant for out of band
                    // delivery of results.  bad things may happen in this case but 
                    // it seems tremendously unlikely, even over the course of a 
                    // VERY long lived client
                    qr = queryRequestFactory.get().createOutOfBandQuery(guid, query, richQuery, type);
                    outOfBandStatistics.addSentQuery();
                } else {                
                    qr = queryRequestFactory.get().createQuery(guid, query, richQuery, type);
                }
            
                recordAndSendQuery(qr, type);
            }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#query(byte[], java.lang.String)
     */
    public void query(byte[] guid, String query) {
        query(guid, query, SearchCategory.ALL);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#query(byte[], java.lang.String, com.limegroup.gnutella.MediaType)
     */
    public void query(byte[] guid, String query, SearchCategory type) {
    	query(guid, query, "", type);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#newQueryGUID()
     */
    public byte[] newQueryGUID() {
        byte []ret;
        if (networkManager.get().isOOBCapable() && outOfBandStatistics.isOOBEffectiveForMe())
            ret = GUID.makeAddressEncodedGuid(networkManager.get().getAddress(), networkManager.get().getPort());
        else
            ret = GUID.makeGuid();
        if (MessageSettings.STAMP_QUERIES.getValue())
            GUID.timeStampGuid(ret);
        return ret;
    }
    
    /**
     * Mutates a query string by shuffling the words and removing trivial words.
     * The returned string may or may not differ from the argument.
     */
    @Override
    public String mutateQuery(String query) {
        return QueryUtils.mutateQuery(query);
    }
    
    @Override
    public BrowseHostHandler doAsynchronousBrowseHost(final FriendPresence friendPresence, GUID guid, final BrowseListener browseListener) {
        final BrowseHostHandler handler = browseHostHandlerManager.createBrowseHostHandler(guid);
        ThreadExecutor.startThread(new DebugRunnable(new Runnable() {
            public void run() {
                handler.browseHost(friendPresence, browseListener);
            }
        }), "BrowseHoster" );
        return handler;
    }
}
