package org.limewire.ui.swing.filter;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * A MatcherEditor used to filter items.  FilterMatcherEditor accepts an 
 * arbitrary Matcher.
 */
class FilterMatcherEditor<E extends FilterableItem> extends AbstractMatcherEditor<E> {
    
    /**
     * Constructs a FilterMatcherEditor with the default Matcher.
     */
    public FilterMatcherEditor() {
    }

    /**
     * Sets the specified matcher, and notifies listeners that the matcher has
     * changed.  If <code>matcher</code> is null, then the default Matcher is
     * applied.
     */
    public void setMatcher(Matcher<E> matcher) {
        if (matcher != null) {
            fireChanged(matcher);
        } else {
            fireMatchAll();
        }
    }
}
