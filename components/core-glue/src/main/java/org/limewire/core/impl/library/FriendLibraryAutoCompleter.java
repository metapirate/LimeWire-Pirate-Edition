package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.Iterator;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;

/**
 * Provides suggestions for a query based on category and what the user's friends
 * have in their libraries for that category.
 */
class FriendLibraryAutoCompleter implements AutoCompleteDictionary {
    private final FriendLibraries friendLibraries;

    private final SearchCategory category;

    public FriendLibraryAutoCompleter(FriendLibraries friendLibraries, SearchCategory category) {
        this.friendLibraries = friendLibraries;
        this.category = category;
    }
    
    @Override
    public boolean isImmediate() {
        return false;
    }

    public void addEntry(String entry) {
        throw new UnsupportedOperationException();
    }

    public boolean removeEntry(String entry) {
        throw new UnsupportedOperationException();
    }

    public String lookup(String prefix) {
        try {
            Iterator<String> it = getPrefixedBy(prefix).iterator();
            if (!it.hasNext())
                return null;
            return it.next();
        } catch(InterruptedException it) {
            throw new IllegalStateException(it);
        }
    }

    public Iterator<String> iterator() {
        try {
            return getPrefixedBy("").iterator();
        } catch(InterruptedException it) {
            throw new IllegalStateException(it);
        }
    }

    public Collection<String> getPrefixedBy(String prefix) throws InterruptedException {
        return friendLibraries.getSuggestions(prefix, category);
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}
