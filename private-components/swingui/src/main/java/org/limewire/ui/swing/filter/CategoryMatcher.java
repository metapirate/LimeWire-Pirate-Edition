package org.limewire.ui.swing.filter;

import org.limewire.core.api.Category;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * A matcher used to filter a search result by category.
 */
class CategoryMatcher<E extends FilterableItem> implements Matcher<E> {
    private final Category category;
    
    /**
     * Constructs a CategoryMatcher for the specified category.
     */
    public CategoryMatcher(Category category) {
        this.category = category;
    }

    /**
     * Returns true if the specified search result matches the category.
     */
    @Override
    public boolean matches(E item) {
        return (item.getCategory() == category);
    }
}
