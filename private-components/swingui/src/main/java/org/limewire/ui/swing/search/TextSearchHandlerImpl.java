package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SearchResultsModelFactory;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The implementation of SearchHandler used to initiate regular text searches.
 * Calling the <code>doSearch(SearchInfo)</code> method will create a new  
 * search results tab in the UI, and start the search operation.
 */
@Singleton
class TextSearchHandlerImpl implements SearchHandler {
    
    private final SearchFactory searchFactory;
    private final Provider<SearchResultsPanelFactory> searchResultPanelFactory;
    private final SearchNavigator searchNavigator;
    private final Provider<SearchResultsModelFactory> searchResultsModelFactory;
    private final LifeCycleManager lifeCycleManager;
    private final GnutellaConnectionManager connectionManager;
    
    private SearchResultsModelFactory modelFactory;
    private SearchResultsPanelFactory panelFactory;
    
    /**
     * Constructs a TextSearchHandlerImpl with the specified services and
     * factories.
     */
    @Inject
    TextSearchHandlerImpl(SearchFactory searchFactory,
            Provider<SearchResultsPanelFactory> searchResultPanelFactory,
            SearchNavigator searchNavigator,
            Provider<SearchResultsModelFactory> searchResultsModelFactory,
            LifeCycleManager lifeCycleManager, 
            GnutellaConnectionManager connectionManager) {
        this.searchNavigator = searchNavigator;
        this.searchFactory = searchFactory;
        this.searchResultPanelFactory = searchResultPanelFactory;
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.lifeCycleManager = lifeCycleManager;
        this.connectionManager = connectionManager;
    }

    /**
     * Performs a search operation using the specified SearchInfo object.  
     * The method always returns true.
     */
    @Override
    public boolean doSearch(final SearchInfo info) {
        // Create search request.
        Search search = searchFactory.createSearch(info);
        
        String panelTitle = info.getTitle();
        
        if(modelFactory == null)
            modelFactory = searchResultsModelFactory.get();
        if(panelFactory == null)
            panelFactory = searchResultPanelFactory.get();
        // Create search results data model and display panel.
        SearchResultsModel searchModel = modelFactory.createSearchResultsModel(info, search);
        SearchResultsPanel searchPanel = panelFactory.createSearchResultsPanel(searchModel);
        
        // Add search results display to the UI, and select its navigation item.
        SearchNavItem item = searchNavigator.addSearch(panelTitle, searchPanel, search, searchModel);
        item.select();

        // Add listeners for connection events.
        addConnectionWarnings(search, searchPanel, item);
        
        // Start search operation.
        startSearch(searchModel, searchPanel, item);
        return true;
    }

    /**
     * Initiates a search using the specified search model, search panel,
     * and navigation item.
     */
    private void startSearch(final SearchResultsModel searchModel, 
            final SearchResultsPanel searchPanel, final SearchNavItem navItem) {
        // prevent search from starting until lifecycle manager completes loading
        if (lifeCycleManager.isStarted()) {
            searchModel.start(new SwingSearchListener(searchModel, navItem));
        } else {
             searchPanel.setLifeCycleComplete(false);
             final EventListener<LifeCycleEvent> listener = new EventListener<LifeCycleEvent>() {
                 @SwingEDTEvent
                 public void handleEvent(LifeCycleEvent event) {
                     if(event == LifeCycleEvent.STARTED) {
                         searchPanel.setLifeCycleComplete(true);
                         searchModel.start(new SwingSearchListener(searchModel, navItem));
                         lifeCycleManager.removeListener(this);
                     }
                 }
             };
             lifeCycleManager.addListener(listener);
             navItem.addNavItemListener(new NavItemListener() {                 
                 public void itemRemoved(boolean wasSelected) {
                     lifeCycleManager.removeListener(listener);
                 }
                 
                 public void itemSelected(boolean selected) {}
             });
        }
    }
    
    /**
     * Notifies the specified SearchResultsPanel if the specified
     * ConnectionStrength indicates a full connection.  Returns true if fully
     * connected, false otherwise.
     */
    private boolean setConnectionStrength(ConnectionStrength type, SearchResultsPanel searchPanel) {
        switch(type) {
        case TURBO:
        case FULL:
            searchPanel.setFullyConnected(true);
            return true;
        }
        
        return false;
    }
    
    /**
     * Removes the specified listeners from the Search request and connection 
     * manager.
     */
    private void removeListeners(Search search, AtomicReference<SearchListener> searchListenerRef, AtomicReference<PropertyChangeListener> connectionListenerRef) {
        SearchListener searchListener = searchListenerRef.get();
        if(searchListener != null) {
            search.removeSearchListener(searchListener);
            searchListenerRef.set(null);
        }
        
        PropertyChangeListener connectionListener = connectionListenerRef.get();
        if(connectionListener != null) {
            connectionManager.removePropertyChangeListener(connectionListener);
            connectionListenerRef.set(null);
        }
    }

    /**
     * Adds listeners to the Search request and connection manager to update 
     * the UI based on connection events.
     */
    private void addConnectionWarnings(final Search search, 
            final SearchResultsPanel searchPanel, NavItem navItem) {
        // Define atomic listener references.
        final AtomicReference<SearchListener> searchListenerRef = new AtomicReference<SearchListener>();
        final AtomicReference<PropertyChangeListener> connectionListenerRef = new AtomicReference<PropertyChangeListener>();

        // Create property change listener for connection strength.  This
        // updates an indicator in SearchResultsPanel, and removes all 
        // warning listeners when the strength is full.
        connectionListenerRef.set(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("strength")) {
                    if(setConnectionStrength((ConnectionStrength)evt.getNewValue(), searchPanel)) {
                        removeListeners(search, searchListenerRef, connectionListenerRef);
                    }
                }
            }
        });  

        // Create search listener to monitor number of results.  If a certain
        // number of search results comes in, for now 10, we assume that while
        // not fully connected, we have a good enough search going so we remove
        // all warning listeners.
        searchListenerRef.set(new SearchListener() {
            private final AtomicInteger numberOfResults = new AtomicInteger(0);
            
            @Override
            public void handleSearchResult(Search search, SearchResult searchResult) {
                if(numberOfResults.addAndGet(1) > 10) {
                    SwingUtils.invokeNowOrLater(new Runnable() {
                        public void run() {
                            // while not fully connected, assume the
                            // connections we have are enough
                            // based on the number of results coming in.
                            searchPanel.setFullyConnected(true);
                        }
                    });
                    removeListeners(search, searchListenerRef, connectionListenerRef);
                }
            }
            
            @Override
            public void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults) {
                if(numberOfResults.addAndGet(searchResults.size()) > 10) {
                    SwingUtils.invokeNowOrLater(new Runnable() {
                        public void run() {
                            // while not fully connected, assume the
                            // connections we have are enough
                            // based on the number of results coming in.
                            searchPanel.setFullyConnected(true);
                        }
                    });
                    removeListeners(search, searchListenerRef, connectionListenerRef);
                }
            }

            @Override public void searchStarted(Search search) {}
            @Override public void searchStopped(Search search) {}                
        });
        
        // Initialize search panel indicator, and install connection listener.
        searchPanel.setFullyConnected(false);
        connectionManager.addPropertyChangeListener(connectionListenerRef.get());

        // If not fully connected, add search listener and navigation listener.
        if(setConnectionStrength(connectionManager.getConnectionStrength(), searchPanel)) {
            removeListeners(search, searchListenerRef, connectionListenerRef);
        } else {
            search.addSearchListener(searchListenerRef.get());
            navItem.addNavItemListener(new NavItemListener() {
                @Override
                public void itemRemoved(boolean wasSelected) {
                    removeListeners(search, searchListenerRef, connectionListenerRef);
                }
                @Override public void itemSelected(boolean selected) {}
            });
        }
    }
}
