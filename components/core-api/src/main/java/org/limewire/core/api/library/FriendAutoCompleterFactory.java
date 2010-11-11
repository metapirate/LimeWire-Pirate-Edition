package org.limewire.core.api.library;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;

/**
 * Responsible for creating the various friend autocompleters.
 */
public interface FriendAutoCompleterFactory {

    /**
     * Returns a FriendLibraryAutocompleter that will supply suggestions based
     * on category.
     */
    public abstract AutoCompleteDictionary getDictionary(SearchCategory categoryToSearch);

    /**
     * Returns a FriendLibraryPropertyAutocompleter that will supply suggestions
     * based on category and FilePropertyKey combination.
     */
    public abstract AutoCompleteDictionary getDictionary(SearchCategory categoryToSearch,
            FilePropertyKey filePropertyKey);

}