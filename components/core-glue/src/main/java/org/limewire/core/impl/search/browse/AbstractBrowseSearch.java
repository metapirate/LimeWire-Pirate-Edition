package org.limewire.core.impl.search.browse;

import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseStatusListener;

abstract class AbstractBrowseSearch implements BrowseSearch {
    protected final CopyOnWriteArrayList<SearchListener> searchListeners = new CopyOnWriteArrayList<SearchListener>();
    protected final CopyOnWriteArrayList<BrowseStatusListener> browseStatusListeners = new CopyOnWriteArrayList<BrowseStatusListener>();


    @Override
    public void addSearchListener(SearchListener searchListener) {
        searchListeners.add(searchListener);
    }

    @Override
    public void removeSearchListener(SearchListener searchListener) {
        searchListeners.remove(searchListener);
    }
    
    @Override
    public void addBrowseStatusListener(BrowseStatusListener listener) {
        browseStatusListeners.add(listener);
    }

    @Override
    public void removeBrowseStatusListener(BrowseStatusListener listener) {
        browseStatusListeners.remove(listener);
    }

    @Override
    public SearchCategory getCategory() {
        return SearchCategory.ALL;
    }

}
