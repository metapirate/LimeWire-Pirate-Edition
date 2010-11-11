package org.limewire.core.impl.search;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchEvent;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.library.FriendSearcher;
import org.limewire.core.impl.search.torrentweb.TorrentWebSearchFactory;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public class CoreSearch implements Search, SearchListener {

    private final SearchDetails searchDetails;
    private final SearchServices searchServices;
    private final QueryReplyListenerList listenerList;
    private final FriendSearcher friendSearcher;
    private final RemoteFileDescAdapter.Factory remoteFileDescAdapterFactory;

    /**
     * A search is considered processed when it is acted upon (started or stopped)
     * <pre>
     * -cannot repeat a search that has not yet been processed
     * -cannot start a search that has already been processed
     * -stopping a search only stops searches that have already been processed.
     * </pre>
     */
    final AtomicBoolean processingStarted = new AtomicBoolean(false);

    private final CopyOnWriteArrayList<SearchListener> searchListeners = new CopyOnWriteArrayList<SearchListener>();
    private final QrListener qrListener = new QrListener();
    private final FriendSearchListener friendSearchListener = new FriendSearchListenerImpl();
    private final ScheduledExecutorService backgroundExecutor;
    private final EventBroadcaster<SearchEvent> searchEventBroadcaster;
    private final AdvancedQueryStringBuilder compositeQueryBuilder;
    
    /**
     * The guid of the last active search.
     */
    volatile byte[] searchGuid;
    private final TorrentWebSearchFactory torrentWebSearchFactory;
    
    private volatile Search torrentWebSearch;

    @Inject
    public CoreSearch(@Assisted SearchDetails searchDetails,
            SearchServices searchServices,
            QueryReplyListenerList listenerList,
            FriendSearcher friendSearcher,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            EventBroadcaster<SearchEvent> searchEventBroadcaster,
            LimeXMLDocumentFactory xmlDocumentFactory,
            AdvancedQueryStringBuilder compositeQueryBuilder,
            RemoteFileDescAdapter.Factory remoteFileDescAdapterFactory,
            TorrentWebSearchFactory googleTorrentSearchFactory) {
        this.searchDetails = searchDetails;
        this.searchServices = searchServices;
        this.listenerList = listenerList;
        this.friendSearcher = friendSearcher;
        this.backgroundExecutor = backgroundExecutor;
        this.searchEventBroadcaster = searchEventBroadcaster;
        this.compositeQueryBuilder = compositeQueryBuilder;
        this.remoteFileDescAdapterFactory = remoteFileDescAdapterFactory;
        this.torrentWebSearchFactory = googleTorrentSearchFactory;
    }
    
    @Override
    public SearchCategory getCategory() {
        return searchDetails.getSearchCategory();
    }
    
    @Override
    public void addSearchListener(SearchListener searchListener) {
        searchListeners.add(searchListener);
    }
    
    @Override
    public void removeSearchListener(SearchListener searchListener) {
        searchListeners.remove(searchListener);
    }

    @Override
    public void start() {
        if (processingStarted.getAndSet(true)) {
            throw new IllegalStateException("cannot start search which has already been processed!");
        }
        
        for(SearchListener listener : searchListeners) {
            listener.searchStarted(this);
        }

        doSearch(true);
    }
    
    private void doSearch(boolean initial) {
        searchEventBroadcaster.broadcast(new SearchEvent(this, SearchEvent.Type.STARTED));
        
        searchGuid = searchServices.newQueryGUID();
        listenerList.addQueryReplyListener(searchGuid, qrListener);
        
        switch(searchDetails.getSearchType()) {
        case KEYWORD:
            doKeywordSearch(initial);
            break;
        case WHATS_NEW:
            doWhatsNewSearch(initial);
            break;
        }
    }
    
    private void doWhatsNewSearch(boolean initial) {
        searchServices.queryWhatIsNew(searchGuid,
                searchDetails.getSearchCategory());
        
        // TODO: Search friends too.
    }
    
    private void doKeywordSearch(boolean initial) {
        String query = searchDetails.getSearchQuery();
        String advancedQuery = "";
        Map<FilePropertyKey, String> advancedSearch = searchDetails.getAdvancedDetails();
        if(advancedSearch != null && advancedSearch.size() > 0) {
            if(query == null || query.equals("")) {
                query = compositeQueryBuilder.createSimpleCompositeQuery(advancedSearch);
            }
            advancedQuery = compositeQueryBuilder.createXMLQueryString(advancedSearch, searchDetails.getSearchCategory().getCategory());
        }
        
        String mutated = searchServices.mutateQuery(query);
        searchServices.query(searchGuid, mutated, advancedQuery,
                searchDetails.getSearchCategory());
        
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() { 
                friendSearcher.doSearch(searchDetails, friendSearchListener);
            }
        });        
        
        if (initial 
                && SearchSettings.USE_TORRENT_WEB_SEARCH.getValue()
                && searchDetails.getSearchCategory() == SearchCategory.TORRENT) {
            torrentWebSearch = torrentWebSearchFactory.create(searchDetails.getSearchQuery());
            torrentWebSearch.addSearchListener(this);
            torrentWebSearch.start();
        }
    }
    
    /**
     * Stops current search and repeats search.
     * 
     * @throws IllegalStateException If search processing has already begun (started or stopped)
     */
    @Override
    public void repeat() {
        if(!processingStarted.get()) {
            throw new IllegalStateException("must start!");
        }
        
        stop();
        
        for(SearchListener listener : searchListeners) {
            listener.searchStarted(CoreSearch.this);
        }
        
        doSearch(false);
    }

    @Override
    public void stop() {
        if(!processingStarted.compareAndSet(true, true)) {
            return;
        }
        
        if (torrentWebSearch != null) {
            torrentWebSearch.stop();
        }
        
        searchEventBroadcaster.broadcast(new SearchEvent(this, SearchEvent.Type.STOPPED));
        listenerList.removeQueryReplyListener(searchGuid, qrListener);
        searchServices.stopQuery(new GUID(searchGuid));
        
        for(SearchListener listener : searchListeners) {
            listener.searchStopped(CoreSearch.this);
        }
    }
    
    public GUID getQueryGuid() {
        return new GUID(searchGuid);
    }
    
    @Override
    public void handleSearchResult(Search search, SearchResult searchResult) {
        for (SearchListener listener : searchListeners) {
            listener.handleSearchResult(this, searchResult);
        }   
    }
    
    @Override
    public void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults) {
        for (SearchListener listener : searchListeners) {
            listener.handleSearchResults(this, searchResults);
        }
    }

    @Override
    public void searchStarted(Search search) {        
    }

    @Override
    public void searchStopped(Search search) {
    }
    
    private class QrListener implements QueryReplyListener {
        @Override
        public void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply,
                Set<? extends IpPort> locs) {
            
            RemoteFileDescAdapter rfdAdapter = remoteFileDescAdapterFactory.create(rfd, locs);
            
            handleSearchResult(CoreSearch.this, rfdAdapter);
        }
    }

    private class FriendSearchListenerImpl implements FriendSearchListener {
        public void handleFriendResults(Collection<SearchResult> results) {
            handleSearchResults(CoreSearch.this, results);
        }
    }

}