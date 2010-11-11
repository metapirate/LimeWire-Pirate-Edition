package org.limewire.core.api.library;

import java.util.Collection;
import java.util.Collections;

import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.DefaultSourceTypeEvent;

/**
 * Event fired by {@link RemoteLibrary} when either its state changes, it is cleared
 * or results are added to it.
 */
public class RemoteLibraryEvent extends DefaultSourceTypeEvent<RemoteLibrary, RemoteLibraryEvent.Type> {

    private final RemoteLibraryState state;
    private final Collection<SearchResult> addedResults;
    private final int startIndex;

    public static enum Type { RESULTS_CLEARED, RESULTS_ADDED, RESULTS_REMOVED, STATE_CHANGED }
    
    private RemoteLibraryEvent(RemoteLibrary source, Type type, Collection<SearchResult> addedResults, int startIndex) { 
        super(source, type);
        this.startIndex = startIndex;
        this.state = source.getState();
        this.addedResults = addedResults;
    }
    
    public static RemoteLibraryEvent createStateChangedEvent(RemoteLibrary remoteLibrary) {
        return new RemoteLibraryEvent(remoteLibrary, Type.STATE_CHANGED, Collections.<SearchResult>emptyList(), -1);
    }
    
    public static RemoteLibraryEvent createResultsClearedEvent(RemoteLibrary remoteLibrary) {
        return new RemoteLibraryEvent(remoteLibrary, Type.RESULTS_CLEARED, Collections.<SearchResult>emptyList(), -1);
    }
    
    public static RemoteLibraryEvent createResultsRemovedEvent(RemoteLibrary remoteLibrary) {
        return new RemoteLibraryEvent(remoteLibrary, Type.RESULTS_REMOVED, Collections.<SearchResult>emptyList(), -1);
    }
    
    public static RemoteLibraryEvent createResultsAddedEvent(RemoteLibrary remoteLibrary, Collection<SearchResult> addedResults, int startIndex) {
        return new RemoteLibraryEvent(remoteLibrary, Type.RESULTS_ADDED, addedResults, startIndex);
    }
    
    /**
     * @return the state of the remote library at creation of this event
     */
    public RemoteLibraryState getState() {
        return state;
    }
    
    /**
     * @return the added results if the event is of type {@link Type#RESULTS_ADDED}
     * otherwise empty collection.
     */
    public Collection<SearchResult> getAddedResults() {
        return addedResults;
    }
    
    /**
     * @return the start index in the {@link RemoteLibrary} for added results
     * or -1 if it doesn't apply to the event type
     */
    public int getStartIndex() {
        return startIndex;
    }
}
