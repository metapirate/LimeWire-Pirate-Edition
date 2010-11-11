package org.limewire.core.api.search;

import java.util.Collection;
import java.util.List;

import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.friend.api.Friend;

/**
 * Defines the API for a grouped search result.  The grouped result may be
 * supported by multiple sources.
 */
public interface GroupedSearchResult {

    /**
     * Returns true if at least one source is anonymous (not a friend).
     */
    boolean isAnonymous();
    
    /**
     * Returns the complete file name including extension.
     */
    String getFileName();
    
    /**
     * Returns a Collection of friends that are sources for the item.  The
     * method returns an empty collection if there are no friends.
     */
    Collection<Friend> getFriends();
    
    /**
     * Returns the relevance value of the search result.  
     */
    float getRelevance();
    
    /**
     * Returns a list of SearchResult values associated with this grouped 
     * result.
     */
    List<SearchResult> getSearchResults();
    
    /**
     * Returns a Collection of sources that support the search result.  Each
     * source is represented by a RemoteHost object. 
     */
    Collection<RemoteHost> getSources();
    
    /**
     * Returns a unique identifier for this file.
     */
    URN getUrn();
}
