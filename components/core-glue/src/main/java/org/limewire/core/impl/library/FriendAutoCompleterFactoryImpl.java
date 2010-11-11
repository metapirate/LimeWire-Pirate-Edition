package org.limewire.core.impl.library;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;

import com.google.inject.Inject;

/**
 * Responsible for creating the various friend autocompleters. Supplies the
 * FriendLibraries object at construction time.
 */
public class FriendAutoCompleterFactoryImpl implements FriendAutoCompleterFactory {

    private final FriendLibraries friendLibraries;

    @Inject
    public FriendAutoCompleterFactoryImpl(FriendLibraries friendLibraries) {
        this.friendLibraries = friendLibraries;
    }

    @Override
    public AutoCompleteDictionary getDictionary(SearchCategory categoryToSearch) {
        return new FriendLibraryAutoCompleter(friendLibraries, categoryToSearch);
    }

    @Override
    public AutoCompleteDictionary getDictionary(SearchCategory categoryToSearch,
            FilePropertyKey filePropertyKey) {
        return new FriendLibraryPropertyAutoCompleter(friendLibraries, categoryToSearch,
                filePropertyKey);
    }

}
