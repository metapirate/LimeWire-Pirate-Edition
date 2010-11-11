package org.limewire.ui.swing.filter;

import java.util.Collection;

import org.limewire.friend.api.Friend;
import org.limewire.util.Objects;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher used to filter a search result by its source.
 */
class SourceMatcher<E extends FilterableItem> implements Matcher<E> {
    /** Source item to filter. */
    private final SourceItem sourceItem;

    /**
     * Constructs a SourceMatcher for the specified source item.
     */
    public SourceMatcher(SourceItem sourceItem) {
        this.sourceItem = sourceItem;
    }

    /**
     * Returns true if the specified filterable item matches the source item.
     */
    @Override
    public boolean matches(E item) {
        switch (sourceItem.getType()) {
        case ANONYMOUS:
            return item.isAnonymous();
            
        case ANY_FRIEND:
            return (item.getFriends().size() > 0);
            
        case FRIEND:
        default:
            // Compare friend names against SourceItem name.
            Collection<Friend> friends = item.getFriends();
            String name = sourceItem.getName();
            for (Friend friend : friends) {
                if (Objects.compareToNullIgnoreCase(name, friend.getRenderName(), false) == 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
