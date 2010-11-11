package org.limewire.ui.swing.search;

/** A component that spawns a new search tab & starts the actual search. */
public interface SearchHandler {

    /** Performs the search & displays it in the UI. */
    boolean doSearch(SearchInfo info);
}