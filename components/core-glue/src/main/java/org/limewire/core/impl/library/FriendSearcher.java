package org.limewire.core.impl.library;

import java.util.Collection;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.FriendSearchListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendSearcher {
    private final FriendLibraries libraries;

    @Inject
    FriendSearcher(FriendLibraries libraries) {
        this.libraries = libraries;
    }
    
    public void doSearch(SearchDetails searchDetails, FriendSearchListener listener) {
        Collection<SearchResult> results = libraries.getMatchingItems(searchDetails);
        listener.handleFriendResults(results);
    }
}
