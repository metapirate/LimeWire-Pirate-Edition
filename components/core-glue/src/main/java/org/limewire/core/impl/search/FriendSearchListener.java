package org.limewire.core.impl.search;

import java.util.Collection;

import org.limewire.core.api.search.SearchResult;

public interface FriendSearchListener {
    public void handleFriendResults(Collection<SearchResult> results);
}
