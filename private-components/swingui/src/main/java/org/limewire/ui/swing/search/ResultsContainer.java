package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Scrollable;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanelFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * ResultsContainer is a display panel that contains the search results tables 
 * for all media categories.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel
 */
public class ResultsContainer extends JXPanel {

    /** Results panel containing display tables. */
    private final BaseResultPanel baseResultPanel;

    /**
     * Constructs a ResultsContainer with the specified search results data
     * model and factories.
     * @see org.limewire.ui.swing.search.ResultsContainerFactory
     */
    @Inject
    ResultsContainer(
        @Assisted SearchResultsModel searchResultsModel,
        BaseResultPanelFactory baseFactory) {
        
        // Create result panel.
        baseResultPanel = baseFactory.create(searchResultsModel);
        // set the default view type.
        baseResultPanel.setViewType(SearchViewType.getSearchViewType(searchResultsModel.getSearchCategory()));
        
        setLayout(new BorderLayout());
        
        // Add result panel to the container.
        add(baseResultPanel, BorderLayout.CENTER);
    }

    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setViewType(SearchViewType mode) {
        baseResultPanel.setViewType(mode);
    }
    
    /**
     * Displays the search results tables for the specified search category.
     */
    public void showCategory(SearchCategory category) {
        baseResultPanel.showCategory(category);
    }

    /**
     * Returns the header component for the category results currently 
     * displayed.  The method returns null if no header is displayed.
     */
    public Component getScrollPaneHeader() {
        return baseResultPanel.getScrollPaneHeader();
    }

    /**
     * Returns the results view component currently being displayed. 
     */
    public Scrollable getScrollable() {
        return baseResultPanel.getScrollable();
    }
}
