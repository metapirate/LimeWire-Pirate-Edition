package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Implementation of factory to create instances of VisualSearchResult.  To
 * improve performance, we provide our own concrete implementation instead of
 * using a Guice injector to construct the object.
 */
public class VisualSearchResultFactoryImpl implements VisualSearchResultFactory {

    private final Provider<PropertiableHeadings> propertiableHeadings;

    /**
     * Constructs a VisualSearchResultFactoryImpl with the specified services.
     */
    @Inject
    public VisualSearchResultFactoryImpl(
            Provider<PropertiableHeadings> propertiableHeadings) {
        this.propertiableHeadings = propertiableHeadings;
    }

    /**
     * Creates a VisualSearchResult for the specified grouped search result.
     */
    @Override
    public VisualSearchResult create(GroupedSearchResult gsr, 
            VisualSearchResultStatusListener listener) {
        SearchResultAdapter vsr = new SearchResultAdapter(gsr, propertiableHeadings, listener);
        return vsr;
    }
}
