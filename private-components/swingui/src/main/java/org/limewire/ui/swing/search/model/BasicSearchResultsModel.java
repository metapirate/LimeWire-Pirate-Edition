package org.limewire.ui.swing.search.model;

import static org.limewire.ui.swing.search.model.VisualSearchResult.NEW_SOURCES;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.DisposalListener;
import org.limewire.ui.swing.filter.FilterDebugger;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.DownloadExceptionHandler;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransactionList;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor.Event;
import ca.odell.glazedlists.util.concurrent.Lock;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Provider;

/**
 * The default implementation of SearchResultsModel containing the results of
 * a search.  This assembles search results into grouped, filtered, and sorted
 * lists, provides access to details about the search request, and handles 
 * requests to download a search result.
 */
class BasicSearchResultsModel implements SearchResultsModel, VisualSearchResultStatusListener {
    private static final Log LOG = LogFactory.getLog(BasicSearchResultsModel.class);
    
    private int downloadsStarted  = 0;
    
    /** Filter debugger associated with this model. */
    private final FilterDebugger<VisualSearchResult> filterDebugger;

    /** Descriptor containing search details. */
    private final SearchInfo searchInfo;
    
    /** Search request object. */
    private final Search search;
    
    /** Search manager. */
    private final SearchManager searchManager;
    
    /** Search result list. */
    private final SearchResultList searchResultList;

    /** Core download manager. */
    private final DownloadListManager downloadListManager;

    /** Download exception handler. */
    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;

    /** Factory to create visual search results. */
    private final VisualSearchResultFactory vsrFactory;

    /** List of visual search results grouped and sorted by URN. */
    private final EventList<VisualSearchResult> groupedUrnResults;

    /** A TransactionList for upstream changes. */
    private final TransactionList<VisualSearchResult> transactionList;

    /** Filtered list of grouped search results. */
    private final FilterList<VisualSearchResult> filteredResultList;

    /** Listener to handle search request events. */
    private SearchListener searchListener;

    /** Listener to handle search result list events. */
    private EventListener<Collection<GroupedSearchResult>> searchListListener;

    /** Current list of sorted and filtered results. */
    private SortedList<VisualSearchResult> sortedResultList;

    /** Current list of visible, sorted and filtered results. */
    private FilterList<VisualSearchResult> visibleResultList;

    /** Current selected search category. */
    private SearchCategory selectedCategory;
    
    /** Current sort option. */
    private SortOption sortOption;

    /** Current matcher editor for filtered search results. */
    private MatcherEditor<VisualSearchResult> filterEditor;

    /** Listener for filter editor. */
    private MatcherEditor.Listener<VisualSearchResult> filterEditorListener;

    /** Matcher editor for visible search results. */
    private final VisibleMatcherEditor visibleEditor = new VisibleMatcherEditor();
    
    private List<DisposalListener> disposalListeners = new ArrayList<DisposalListener>();
    
    /** A list of listeners for changes. */
    private final List<VisualSearchResultStatusListener> changeListeners;
    
    /** A queue used to collect search results and apply them in bulk. */
    private final ListQueuer listQueuer = new ListQueuer();
    
    /** Comparator that searches through the list of results & finds them based on URN. */
    private final UrnResultFinder resultFinder = new UrnResultFinder();
    
    //TODO Using this to fix a case where events are coming in for items no longer in the list after a clear.
    //We should remove this after the release and fix the root cause, that DownloadListeners for visual search results 
    //are not being removed from the downloaders after search tabs are removed or refreshed. 
    private boolean cleared = false;

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     */
    public BasicSearchResultsModel(SearchInfo searchInfo, Search search, 
            VisualSearchResultFactory vsrFactory,
            DownloadListManager downloadListManager,
            Provider<DownloadExceptionHandler> downloadExceptionHandler,
            SearchManager searchManager) {
        
        this.searchInfo = searchInfo;
        this.search = search;
        this.vsrFactory = vsrFactory;
        this.searchManager = searchManager;
        this.downloadListManager = downloadListManager;
        this.downloadExceptionHandler = downloadExceptionHandler;
        
        changeListeners = new ArrayList<VisualSearchResultStatusListener>(3);
        
        // Create filter debugger.
        filterDebugger = new FilterDebugger<VisualSearchResult>();
        
        // Create core list of grouped results.
        searchResultList = searchManager.addSearch(search, searchInfo);
        
        // Create local list of grouped visual results.  This list is always
        // updated on the EDT, so we optimize performance by using a dummy lock.
        groupedUrnResults = new BasicEventList<VisualSearchResult>(new ReadWriteLock() {
            private final Lock noopLock = new Lock() {
                @Override public void lock() {}
                @Override public boolean tryLock() { return true; }
                @Override public void unlock() {}
            };
            
            @Override public Lock readLock() {
                return noopLock;
            }
            
            @Override public Lock writeLock() {
                return noopLock;
            }
        });
        
        // Create transaction list.
        transactionList = GlazedListsFactory.transactionList(groupedUrnResults);
        
        // Create filtered list. 
        filteredResultList = GlazedListsFactory.filterList(transactionList);
        
        // Initialize display category and sorted list.
        setSelectedCategory(searchInfo.getSearchCategory());
    }

    /**
     * Installs the specified search listener and starts the search.  The
     * search listener should handle search results by calling the 
     * <code>addSearchResult(SearchResult)</code> method.
     */
    @Override
    public void start(SearchListener searchListener) {
        if (searchListener == null) {
            throw new IllegalArgumentException("Search listener cannot be null");
        }
        
        // Install search listener.
        this.searchListener = searchListener;
        search.addSearchListener(searchListener);
        
        // Install listener to handle new grouped results.  The event is 
        // usually fired by a background thread.
        searchListListener = new EventListener<Collection<GroupedSearchResult>>() {
            @Override
            public void handleEvent(Collection<GroupedSearchResult> results) {
                // Add results to queue for processing.  The queue is used to
                // improve performance for Browse Host requests, which can
                // generate thousands of results one at a time.
                listQueuer.addAll(results);
            }
        };
        searchResultList.addListener(searchListListener);
        
        // Start search.
        search.start();
    }
    
    /**
     * Adds the specified list of grouped results to the list of visual
     * results.
     */
    void addResultsInternal(final List<GroupedSearchResult> resultList) {
        // Process next group of results.  We use a transaction list so that
        // large result sets generate a single list change event.  This
        // improves the performance for Browse Host/Friend requests, which can
        // generate thousands of results.
        transactionList.beginEvent(true);
        try {
            for (GroupedSearchResult gsr : resultList) {
                URN urn = gsr.getUrn();
                int idx = Collections.binarySearch(groupedUrnResults, urn, resultFinder);
                if (idx < 0) {
                    // URN not found so add visual result at insertion point.
                    // This keeps the list in sorted order.
                    idx = -(idx + 1);
                    VisualSearchResult vsr = vsrFactory.create(gsr, this);
                    groupedUrnResults.add(idx, vsr);
                    // Notify listeners to update result states.
                    for (VisualSearchResultStatusListener listener : changeListeners) {
                        listener.resultCreated(vsr);
                    }
                } else {
                    // URN found so replace result to generate change event.
                    VisualSearchResult vsr = groupedUrnResults.get(idx);
                    groupedUrnResults.set(idx, vsr);
                    // Notify listeners to update similar results.
                    for (VisualSearchResultStatusListener listener : changeListeners) {
                        listener.resultChanged(vsr, NEW_SOURCES, null, null);
                    }
                }
            }
        } finally {
            transactionList.commitEvent();
        }

        // Reapply sort in case similarity parents have changed.
        if (sortedResultList != null) {
            sortedResultList.setComparator(sortedResultList.getComparator());
        }
    }
    
    /**
     * Stops the search and removes the current search listener. 
     */
    @Override
    public void dispose() {
        
        // Stop search.
        search.stop();
        
        // Remove search listeners.
        if (searchListener != null) {
            search.removeSearchListener(searchListener);
            searchListener = null;
        }
        if (searchListListener != null) {
            searchResultList.removeListener(searchListListener);
            searchListListener = null;
        }
        
        // Remove search from core management.
        searchManager.removeSearch(search);
        
        groupedUrnResults.dispose();
        
        notifyDisposalListeners();
    }
    
    @Override
    public SearchCategory getFilterCategory() {
        return searchInfo.getSearchCategory();
    }
    
    @Override
    public FilterDebugger<VisualSearchResult> getFilterDebugger() {
        return filterDebugger;
    }
    
    @Override
    public EventList<VisualSearchResult> getUnfilteredList() {
        return transactionList;
    }
    
    @Override
    public EventList<VisualSearchResult> getFilteredList() {
        return filteredResultList;
    }
    
    @Override
    public SearchCategory getSearchCategory() {
        return searchInfo.getSearchCategory();
    }
    
    @Override
    public String getSearchQuery() {
        return searchInfo.getSearchQuery();
    }
    
    @Override
    public String getSearchTitle() {
        return searchInfo.getTitle();
    }
    
    @Override
    public int getResultCount() {
        return searchResultList.getResultCount();
    }
    
    @Override
    public SearchType getSearchType() {
        return searchInfo.getSearchType();
    }

    /**
     * Returns a list of filtered results.
     */
    @Override
    public EventList<VisualSearchResult> getFilteredSearchResults() {
        return filteredResultList;
    }

    /**
     * Returns a list of sorted and filtered results for the selected search
     * category and sort option.  Only visible results are included in the list.
     */
    @Override
    public EventList<VisualSearchResult> getSortedSearchResults() {
        return visibleResultList;
    }

    /**
     * Returns the selected search category.
     */
    @Override
    public SearchCategory getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Selects the specified search category.  If the selected category is
     * changed, this method updates the sorted list.
     */
    @Override
    public void setSelectedCategory(SearchCategory selectedCategory) {
        if (this.selectedCategory != selectedCategory) {
            this.selectedCategory = selectedCategory;
            updateSortedList();
        }
    }
    
    /**
     * Updates sorted list of visible results.  This method is called when the
     * selected category is changed.
     */
    private void updateSortedList() {
        // Create visible and sorted lists if necessary.
        if (visibleResultList == null) {
            sortedResultList = GlazedListsFactory.sortedList(filteredResultList, sortOption != null ? SortFactory.getSortComparator(sortOption) : null);
            visibleResultList = GlazedListsFactory.filterList(sortedResultList, visibleEditor);
        }
    }
    
    /**
     * Sets the sort option.  This method updates the sorted list by changing 
     * the sort comparator.
     */
    @Override
    public void setSortOption(SortOption sortOption) {
        this.sortOption = sortOption;
        sortedResultList.setComparator((sortOption != null) ? SortFactory.getSortComparator(sortOption) : null);
    }
    
    @Override
    public void setFilterEditor(MatcherEditor<VisualSearchResult> editor) {
        // Remove listener from existing filter editor.
        if ((filterEditor != null) && (filterEditorListener != null)) {
            filterEditor.removeMatcherEditorListener(filterEditorListener);
        }
        
        // Create listener to handle changes to the filter.
        if (filterEditorListener == null) {
            filterEditorListener = new MatcherEditor.Listener<VisualSearchResult>() {
                @Override
                public void changedMatcher(Event<VisualSearchResult> matcherEvent) {
                    // Post Runnable on event queue to update filtered list for
                    // visible items.  This allows us to display child results
                    // whose parents become hidden due to filtering.
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            visibleEditor.update();
                        }
                    });
                }
            };
        }
        
        // Set filter editor and add listener.
        filterEditor = editor;
        filterEditor.addMatcherEditorListener(filterEditorListener);
        
        // Apply filter editor.
        filteredResultList.setMatcherEditor(filterEditor);
    }
    
    public void addResultListener(VisualSearchResultStatusListener vsrChangeListener) {
        changeListeners.add(vsrChangeListener);
    }
    
    @Override
    public void resultCreated(VisualSearchResult vsr) {
    }
    
    @Override
    public void resultsCleared() {
    }
    
    @Override
    public void resultChanged(VisualSearchResult vsr, String propertyName, Object oldValue, Object newValue) {
        // Scan through the list & find the item.
        URN urn = vsr.getUrn();
        int idx = Collections.binarySearch(groupedUrnResults, urn, resultFinder);
        //TODO clean up, see comment about cleared variable, and why it should be removed.
        assert cleared || idx >= 0;

        if (idx >=0) {
            VisualSearchResult existing = groupedUrnResults.get(idx);
            VisualSearchResult replaced = groupedUrnResults.set(idx, existing);
            assert cleared || replaced == vsr;
            if(replaced == vsr) {
                for(VisualSearchResultStatusListener listener : changeListeners) {
                    listener.resultChanged(vsr, propertyName, oldValue, newValue);
                }
            }
        }
    }
    
    /**
     * Removes all results from the model
     */
    @Override
    public void clear() {
        listQueuer.clear();
    }
    
    /**
     * Initiates a download of the specified visual search result.
     */
    @Override
    public void download(VisualSearchResult vsr) {
        download(vsr, null);
    }
    
    /**
     * Initiates a download of the specified visual search result to the
     * specified save file.
     */
    @Override
    public void download(final VisualSearchResult vsr, File saveFile) {
        try {
            
            downloadsStarted++;
            
            // Add download to manager.  If save file is specified, then set
            // overwrite to true because the user has already confirmed it.
            DownloadItem di = (saveFile == null) ?
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults()) :
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, true);
            
            // Add listener, and initialize download state.
            di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
            BasicDownloadState state = BasicDownloadState.fromState(di.getState());
            if(state != null) {
                vsr.setDownloadState(state);
            }
        } catch (final DownloadException e) {
            if (e.getErrorCode() == DownloadException.ErrorCode.FILE_ALREADY_DOWNLOADING) {
                DownloadItem downloadItem = downloadListManager.getDownloadItem(vsr.getUrn());
                if (downloadItem != null) {
                    downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                    BasicDownloadState state = BasicDownloadState.fromState(downloadItem.getState());
                    if(state != null) {
                        vsr.setDownloadState(state);
                    }
                    if (saveFile != null) {
                        try {
                            // Update save file in DownloadItem.
                            downloadItem.setSaveFile(saveFile, true);
                        } catch (DownloadException ex) {
                            LOG.infof(ex, "Unable to relocate downloading file {0}", ex.getMessage());
                        }
                    }
                }
            } else {
                downloadExceptionHandler.get().handleDownloadException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws DownloadException {
                        DownloadItem di = downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, overwrite);
                        di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                        BasicDownloadState state = BasicDownloadState.fromState(di.getState());
                        if(state != null) {
                            vsr.setDownloadState(state);
                        }
                    }

                    @Override
                    public void downloadCanceled(DownloadException ignored) {
                        //nothing to do                        
                    }

                }, e, true);
            }
        }
    }
    
    /**
     * Returns true if the specified search result is allowed by the current
     * filter editor.  This means the result is a member of the filtered list.
     */
    private boolean isFilterMatch(VisualSearchResult vsr) {
        if (filterEditor != null) {
            return filterEditor.getMatcher().matches(vsr);
        }
        return true;
    }

    /**
     * A matcher editor used to filter visible search results. 
     */
    private class VisibleMatcherEditor extends AbstractMatcherEditor<VisualSearchResult> {
        
        public VisibleMatcherEditor() {
            currentMatcher = new Matcher<VisualSearchResult>() {
                @Override
                public boolean matches(VisualSearchResult item) {
                    // Determine whether item has parent, and parent is hidden
                    // due to filtering.  If so, we treat the item as visible.
                    VisualSearchResult parent = item.getSimilarityParent();
                    boolean parentHidden = (parent != null) && !isFilterMatch(parent);
                    return item.isVisible() || parentHidden;
                }
            };
        }
        
        /**
         * Updates the list by firing a matcher change event to registered
         * listeners.
         */
        public void update() {
            fireChangedMatcher(new MatcherEditor.Event<VisualSearchResult>(
                    this, Event.CHANGED, currentMatcher));
        }
    }

    @Override
    public void addDisposalListener(DisposalListener listener) {
        disposalListeners.add(listener);
    }

    @Override
    public void removeDisposalListener(DisposalListener listener) {
        disposalListeners.remove(listener);
    }
    
    private void notifyDisposalListeners(){
        for (DisposalListener listener : disposalListeners){
            listener.objectDisposed(this);
        }
    }
    
    /**
     * A queue used to collect search results and apply them in bulk.
     * ListQueuer is used to protect the performance of UI event processing
     * when thousands of individual results arrive in rapid succession.  When
     * a result is received, it is added to the queue and scheduled for later
     * processing on the event queue.  If another result is received before
     * the event is handled, then it is added to the queue so it can be
     * processed together with other pending results.
     */
    private class ListQueuer implements Runnable {
        private final Object LOCK = new Object();
        /** Queue of pending results; should always be accessed under LOCK. */
        private final List<GroupedSearchResult> queue = new ArrayList<GroupedSearchResult>();
        private final List<GroupedSearchResult> transferQ = new ArrayList<GroupedSearchResult>();
        private boolean scheduled = false;

        /**
         * Adds the specified collection of results to the queue, and schedules
         * an event to add them to the result list.
         */
        void addAll(Collection<? extends GroupedSearchResult> results) {
            synchronized(LOCK) {
                queue.addAll(results);
                schedule();
            }
        }

        /**
         * Clears the queue and posts an event to clear all search results.
         */
        void clear() {
            synchronized(LOCK) {
                queue.clear();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        cleared = true;
                        // Clear result list.
                        searchResultList.clear();
                        groupedUrnResults.clear();
                        // Notify change listeners that results are cleared.
                        for (VisualSearchResultStatusListener listener : changeListeners) {
                            listener.resultsCleared();
                        }
                    }
                });
            }
        }
        
        /**
         * Posts event to process queued results.  This method should only be
         * called while holding the lock. 
         */
        private void schedule() {
            if (!scheduled) {
                scheduled = true;
                // Purposely use SwingUtilities, and not SwingUtils, so that
                // that we force an event on the end of the event queue.
                SwingUtilities.invokeLater(this);
            }
        }
        
        @Override
        public void run() {
            // Move result into transferQ inside the lock.
            transferQ.clear();
            synchronized(LOCK) {
                transferQ.addAll(queue);
                queue.clear();
            }
            
            // Add to search results outside the lock so we don't hold the
            // lock while events are being triggered.
            addResultsInternal(transferQ);
            transferQ.clear();
            
            synchronized(LOCK) {
                scheduled = false;
                // If the queue isn't empty, we need to reschedule ourselves
                // because results got added to the queue without scheduling
                // itself since we had scheduled=true.
                if (!queue.isEmpty()) {
                    schedule();
                }
            }
        }
    }
    
    /**
     * Comparator to search for VisualSearchResult objects by URN.  This is
     * only used to perform a binary search by URN, never to perform the sort,
     * so the compare() method does not need to be symmetric.
     */
    private static class UrnResultFinder implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            return ((VisualSearchResult)o1).getUrn().compareTo(((URN)o2));
        }
    }
}
